package com.example.data.backup

import com.example.data.local.ReviewCardEntity

/**
 * When a Segue School card exists on both sides, keep the later-reviewed copy;
 * higher [ReviewCardEntity.timesSeen] breaks a date tie (H2). Local wins a full tie.
 */
fun preferReviewCard(local: ReviewCardEntity?, backup: ReviewCardEntity): ReviewCardEntity {
    if (local == null) return backup
    return when {
        backup.lastReviewedDate > local.lastReviewedDate -> backup
        backup.lastReviewedDate < local.lastReviewedDate -> local
        backup.timesSeen > local.timesSeen -> backup
        else -> local
    }
}
