package com.tickertape.tickertape_spring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stock_peers")
public class StockPeer {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sid;

    private String name;
    private String ticker;
    private String sector;

    private Double ttmPe;
    private Double breco;

    @Field("52wpct")
    private Double week52Pct;

    private Double marketCap;
    private Double apef;
    private Double divDps;
    private Double pbr;

    @Field("52wHigh")
    private Double week52High;

    @Field("52wLow")
    private Double week52Low;

    @Field("4wpct")
    private Double week4Pct;

    private Double divYield;

    @Field("3mAvgVol")
    private Double avgVolume3m;

    private Double pe;
    private Double pb;
    private Double beta;
    private Double inddy;
    private Double indpb;
    private Double indpe;
    private Double lastPrice;
    private Double risk;
    private Double mrktCapRank;
    private Double eps;
    private Double bps;
    private Double roe;

    @Field("nShareholders")
    private Double shareholderCount;
    private String marketCapLabel;
    private Double mrktCapf;

    @Field("12mVol")
    private Double vol12m;

    private Double expenseRatio;
    private Double trackErr;
    private Double indExpenseRatio;
    private Double indTrackErr;
    private Double asstUnderMan;
    private Double etfLiq;
    private String etfLiqLabel;

    private String type;
    private String exchange;
    private Double price;
    private Double o;
    private Double h;
    private Double l;
    private Double c;
    private Double change;
    private Long vol;
    private Long ts;
    private Double wkRange;
    private Double mnRange;
    private Double dyChange;
    private Double wkChange;
    private Double mnChange;
    private Boolean crossedHigh;
    private Boolean crossedLow;
    private Double away52wH;
    private Double away52wL;
    private Double turnover;
    private Double volBreakout;
    private Double low52w;
    private Double high52w;

    private Instant updatedAt;
}
