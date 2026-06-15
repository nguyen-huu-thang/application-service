package vn.xime.application.application.dto.internal;

/**
 * Query to pull applications changed after a given change_sequence cursor.
 * Query kéo các application thay đổi sau một con trỏ change_sequence.
 *
 * afterSequence = 0 nghĩa là kéo tất cả từ đầu. limit bị use case kẹp về tối đa 200.
 */
public record PollChangedApplicationsQuery(
        long afterSequence,
        int limit
) {
}
