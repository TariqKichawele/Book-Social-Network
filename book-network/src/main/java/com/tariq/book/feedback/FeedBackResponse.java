package com.tariq.book.feedback;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedBackResponse {
    private Double note;
    private String comment;
    private boolean ownFeedback;
}
