package com.puzzleboxremapping;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdjacentIndexes {
    int above;
    int below;
    int left;
    int right;
}
