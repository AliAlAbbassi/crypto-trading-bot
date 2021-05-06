package com.hoop.app;

import com.binance.api.client.BinanceApiClientFactory;

public class App {

    public static void main(String[] args) {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        Bot bot = new Bot(factory);
        bot.run();
    }

}
