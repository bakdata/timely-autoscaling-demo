package com.bakdata.ks23.integrator;


import lombok.Value;

@Value
public class Prediction {
    double score;
    String type;
}
