package com.bakdata.ks23.integrator.client;


import lombok.Value;

@Value
public class Prediction {
    double score;
    String type;
}
