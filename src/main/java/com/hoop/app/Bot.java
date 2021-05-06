package com.hoop.app;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.CandlestickEvent;
import com.binance.api.client.domain.market.CandlestickInterval;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.ConvertibleBaseBarBuilder;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class Bot {
    BinanceApiClientFactory factory;
    BinanceApiWebSocketClient client;

    private int RSI_PERIOD = 14;
    private int RSI_OVERBOUGHT = 70;
    private int RSI_OVERSOLD = 30;
    private String TRADE_SYMBOL = "ethusd";
    private double TRADE_QUANTITY = 0.01;

    private boolean in_Position = false;
    private List<Bar> bars = new ArrayList<Bar>();

    public Bot(BinanceApiClientFactory factory) {
        this.factory = factory;
        this.client = factory.newWebSocketClient();

        // Listen for aggregated trade events for ETH/BTC
        // client.onAggTradeEvent("ethbtc", response -> onMessage(response));

        // Listen for changes in the order book in ETH/BTC
        // client.onDepthEvent("ethbtc", response -> System.out.println(response));

        // Obtain 1m candlesticks in real-time for ETH/BTC
        // client.onCandlestickEvent("ethbtc", CandlestickInterval.ONE_MINUTE, response
        // -> onMessage(response));
    }

    public void run() {
        client.onCandlestickEvent("ethbtc", CandlestickInterval.ONE_MINUTE, new BinanceApiCallback<CandlestickEvent>() {
            @Override
            public void onResponse(CandlestickEvent response) {
                onMessage(response);
            }

            @Override
            public void onFailure(Throwable cause) {
                BinanceApiCallback.super.onFailure(cause);
                System.out.println(cause);
            }
        });
    }

    public void onMessage(CandlestickEvent response) {
        System.out.println("Message Recieved");
        this.bars.add(createBar(response));
        if (this.bars.size() > this.RSI_PERIOD) {
            BarSeries series = new BaseBarSeriesBuilder().withName("Series").withNumTypeOf(DoubleNum::valueOf)
                    .withBars(bars).build();
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            RSIIndicator rsi = new RSIIndicator(closePrice, RSI_PERIOD);
            System.out.println("All RSIs calculated so far");
            System.out.println(rsi.toString());
            Num last_rsi = rsi.getValue(series.getEndIndex());
            System.out.println("The current rsi is " + last_rsi);

            if (last_rsi.intValue() > this.RSI_OVERBOUGHT) {
                if (this.in_Position) {
                    System.out.println("Overbought!");
                    // binance sell logic
                    // NewOrderResponse newOrderResponse = client
                    // .newOrder(limitBuy("LINKETH", TimeInForce.GTC, "1000", "0.0001"));
                    // System.out.println(newOrderResponse.getTransactTime());
                    if (order_Succeeded()) {
                        this.in_Position = false;
                    }
                } else {
                    System.out.println("It's overbought, but we don't own any. Nothing to do here");
                }
            }

            if (last_rsi.intValue() < this.RSI_OVERSOLD) {
                if (this.in_Position) {
                    System.out.println("It is oversold, but you already own it, nothing to do.");
                } else {
                    System.out.println("Oversold!");
                    // put binance buy order logic here
                    // NewOrderResponse newOrderResponse = client
                    // .newOrder(marketBuy("LINKETH",
                    // "1000").orderRespType(OrderResponseType.FULL));
                    // List<Trade> fills = newOrderResponse.getFills();
                    // System.out.println(newOrderResponse.getClientOrderId());
                    if (order_Succeeded()) {
                        this.in_Position = true;
                    }
                }
            }
        }
    }

    public boolean order_Succeeded() {
        return false;
    }

    private Bar createBar(CandlestickEvent response) {
        ZonedDateTime endTime = ZonedDateTime.now();
        return barBuilderFromString().timePeriod(Duration.ofDays(1)).endTime(endTime).openPrice(response.getOpen())
                .highPrice(response.getHigh()).lowPrice(response.getLow()).closePrice(response.getClose())
                .volume(response.getVolume()).build();
    }

    private ConvertibleBaseBarBuilder<String> barBuilderFromString() {
        return BaseBar.builder(DoubleNum::valueOf, String.class);
    }

}
