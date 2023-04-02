package com.bakdata.ks23.rest;


import lombok.Value;

@Value
public class Prediction {
    double score;
    String type;
}
