package com.ymlai87416.stockoption.server.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class StockOption extends Asset{

    private List<StockOptionHistory> historyList;

    public StockOption(Long id, String ticker, String name) {
        super(id, ticker, name);
    }

    public List<StockOptionHistory> getHistoryList(){
        return this.historyList;
    }

    public void setHistoryList(List<StockOptionHistory> value){
        this.historyList = value;
    }

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("MMMyy", Locale.US);

    public char getOptionType(){
        return ticker.charAt(3);
    }

    public Optional<Float> getStrikePrice(){
        try {
            return Optional.of(ticker.substring(4, ticker.length() - 5)).map(Float::parseFloat);
        }
        catch(Exception ex){
            return Optional.empty();
        }
    }

    public Optional<Date> getDateTime(){
        try {
            Date result = dateFormat.parse(ticker.substring(ticker.length() - 5));
            return Optional.of(result);
        }
        catch(Exception ex){
            return Optional.empty();
        }
    }

}
