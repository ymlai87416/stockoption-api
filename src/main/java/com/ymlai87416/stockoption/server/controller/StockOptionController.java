package com.ymlai87416.stockoption.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ymlai87416.stockoption.server.domain.DailyPrice;
import com.ymlai87416.stockoption.server.domain.StockOptionUnderlyingAsset;
import com.ymlai87416.stockoption.server.domain.Symbol;
import com.ymlai87416.stockoption.server.model.StockOption;
import com.ymlai87416.stockoption.server.model.StockOptionHistory;
import com.ymlai87416.stockoption.server.service.*;
import com.ymlai87416.stockoption.server.utilities.Utilities;
import org.hibernate.dialect.Sybase11Dialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.web.bind.annotation.*;

//import javax.rmi.CORBA.Util;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class StockOptionController {

    /**
     * findBySEHKCode                       : /stockOption/sehk/{id}
     * findBySEHKCodeWithHistory            : /stockOption/sehk/{id}?history=1
     * findByOptionCodeWithHistory          : /stockOption/code/{id}?history=1
     * findAvailableDateBySEHKCode          : /stockOption/sehk/{id}/listDate
     * findLatestAvailableDateBySEHKCode    : /stockOption/sehk/{id}/listDate?latest=1
     * getAllStockOption                    : /stockOption
     * getAllStockOptionUnderlyingAsset     : /stockOption/underlyingAsset
     */

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private SymbolRepository symbolRepository;
    private DailyPriceRepository dailyPriceRepository;
    private StockOptionUnderlyingAssetRepository stockOptionUnderlyingAssetRepository;
    List<StockOptionUnderlyingAsset> underlyingAssetsList;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private StockOptionController(SymbolRepository symbolRepository,
                                  DailyPriceRepository dailyPriceRepository,
                                  StockOptionUnderlyingAssetRepository stockOptionUnderlyingAssetRepository
    ){
        this.symbolRepository = symbolRepository;
        this.dailyPriceRepository = dailyPriceRepository;
        this.stockOptionUnderlyingAssetRepository = stockOptionUnderlyingAssetRepository;
        underlyingAssetsList = this.stockOptionUnderlyingAssetRepository.findAll();
    }

    private boolean tickerMatch(String tickerStr, int tickerNum){
        try{
            int parseInt = Integer.parseInt(tickerStr.replace(".HK", ""));
            return parseInt == tickerNum;
        }
        catch(Exception ex){
            return false;
        }
    }

    @RequestMapping("/stockOption/sehk/{id}")
    @CrossOrigin(origins={"http://localhost:4200", "http://stockoption.ymlai87416.com"})
    public List<StockOption> findStockOptionBySEHKCode(@PathVariable String id,
                                                       @RequestParam(value="startDate", required=false) String startDate,
                                                       @RequestParam(value="endDate", required=false) String endDate) throws Exception {
        try {
            //int tickerNum = Integer.parseInt(id);
            Optional<StockOptionUnderlyingAsset> asset = underlyingAssetsList.stream().filter(x -> x.getTicker().compareToIgnoreCase(id) == 0 /*tickerMatch(x.getTicker(), tickerNum)*/).findFirst();

            if (asset.isPresent()) {
                /*
                Symbol symbol = new Symbol();
                symbol.setInstrument("HK Stock Option");
                symbol.setTicker(asset.get().getShortForm());

                ExampleMatcher matcher = ExampleMatcher.matching()
                        .withMatcher("Instrument", x -> x.exact())
                        .withMatcher("Ticker", x -> x.contains());

                Example<Symbol> example  = Example.of(symbol, matcher);
                List<Symbol> searchResult = symbolRepository.findAll(example);
                */
                List<Symbol> searchResult = null;
                List<DailyPrice> childSearchResult = null;
                String tickerPattern = asset.get().getShortForm() + "%";

                Date[] startEndDate = Utilities.parseStartDateAndEndDate(startDate, endDate);

                boolean initChild = false;
                if(startEndDate != null && startEndDate.length == 2
                        && startEndDate[0] != null && startEndDate[1] != null){
                    logger.debug("Before query 1");

                    
                    searchResult = symbolRepository.findByInstrumentEqualsAndTickerLikeAndDailyPriceListPriceDateBetween
                            ("HK Stock Option", tickerPattern, startEndDate[0], startEndDate[1]);

                    logger.debug("After query 1");

                    /*
                    childSearchResult = dailyPriceRepository.findBySymbolInAndPriceDateBetween(searchResult,
                            startEndDate[0], startEndDate[1]);
                    */

                    childSearchResult = dailyPriceRepository.findBySymbolInstrumentEqualsAndSymbolTickerLikeAndPriceDateBetween("HK Stock Option", tickerPattern,
                            startEndDate[0], startEndDate[1]);

                    logger.debug("After query 2");

                    for(Symbol symbol : searchResult){
                        List<DailyPrice> dailyPriceList =
                                childSearchResult.stream().filter(x -> x.getSymbol().getId() == symbol.getId()).collect(Collectors.toList());
                        symbol.setDailyPriceList(dailyPriceList);
                    }

                    logger.debug("After consolidate");

                    initChild = true;
                }
                else{
                    searchResult = symbolRepository.findByInstrumentAndTickerLike("HK Stock Option", tickerPattern);
                }

                if (searchResult != null)
                    return convertToStockOptionList(searchResult, !initChild);
                else
                    return Collections.emptyList();
            } else
                return Collections.emptyList();
        } catch (Exception ex) {
            logger.error("Exception occurred", ex);
            throw new Exception("Invalid SEHK code.");
        }
    }

    @RequestMapping("/stockOption/code/{id}")
    @CrossOrigin(origins={"http://localhost:4200", "http://stockoption.ymlai87416.com"})
    public List<StockOption> findStockOptionByOptionCode(@PathVariable String id,
                                                         @RequestParam(value="startDate", required=false) String startDate,
                                                         @RequestParam(value="endDate", required=false) String endDate)
    {
        /*
        Symbol symbol = new Symbol();
        symbol.setInstrument("HK Stock Option");
        symbol.setTicker(id);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withMatcher("Instrument", x -> x.exact())
                .withMatcher("Ticker", x -> x.exact());

        Example<Symbol> example  = Example.of(symbol, matcher);

        List<Symbol> searchResult = symbolRepository.findAll(example);
        */
        List<Symbol> searchResult;
        List<DailyPrice> childSearchResult = null;

        Date[] startEndDate = Utilities.parseStartDateAndEndDate(startDate, endDate);

        boolean initChild = false;
        if(startEndDate != null && startEndDate.length == 2
                && startEndDate[0] != null && startEndDate[1] != null){
            searchResult = symbolRepository.findByInstrumentEqualsAndTickerAndDailyPriceListPriceDateBetween
                    ("HK Stock Option", id, startEndDate[0], startEndDate[1]);
            childSearchResult = dailyPriceRepository.findBySymbolInAndPriceDateBetween(searchResult,
                    startEndDate[0], startEndDate[1]);

            for(Symbol symbol : searchResult){
                List<DailyPrice> dailyPriceList =
                        childSearchResult.stream().filter(x -> x.getSymbol().getId() == symbol.getId()).collect(Collectors.toList());
                symbol.setDailyPriceList(dailyPriceList);
            }

            initChild = true;
        }
        else{
            searchResult = symbolRepository.findByInstrumentAndTicker("HK Stock Option", id);
        }

        if(searchResult != null)
            return convertToStockOptionList(searchResult, !initChild);
        else
            return Collections.emptyList();
    }

    @RequestMapping("/stockOption/underlyingAsset")
    @CrossOrigin(origins={"http://localhost:4200", "http://stockoption.ymlai87416.com"})
    public List<StockOptionUnderlyingAsset> getAllStockOptionUnderlyingAsset()
    {
        return underlyingAssetsList;
    }

    private List<StockOption> convertToStockOptionList(List<Symbol> symbolList, boolean skipChild){
        return symbolList.stream().map(x -> convertToStockOption(x, skipChild)).collect(Collectors.toList());
    }

    private StockOption convertToStockOption(Symbol symbol, boolean skipChild){
        StockOption result =  new StockOption(symbol.getId(), symbol.getTicker(), symbol.getName());
        if(!skipChild) {
            List<StockOptionHistory> children = convertToStockOptionHistoryList(symbol.getDailyPriceList());
            result.setHistoryList(children);
        }
        return result;
    }

    private List<StockOptionHistory> convertToStockOptionHistoryList(List<DailyPrice> dailyPriceList){
        return dailyPriceList.stream().map(x -> convertToStockOptionHistory(x)).collect(Collectors.toList());
    }

    private StockOptionHistory convertToStockOptionHistory(DailyPrice dailyPrice){
        return new StockOptionHistory(dailyPrice.getId(), dailyPrice.getSymbol().getId(),
                dailyPrice.getPriceDate(), dailyPrice.getOpenPrice(), dailyPrice.getHighPrice(), dailyPrice.getLowPrice(),
                dailyPrice.getClosePrice(), dailyPrice.getOpenInterest(), dailyPrice.getIv());
    }

}
