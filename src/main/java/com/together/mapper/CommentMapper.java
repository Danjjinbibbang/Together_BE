package com.together.mapper;

import com.together.dto.Comment;
import com.together.dto.CommentView;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * COMMENTS 테이블 접근. {@code resources/mapper/CommentMapper.xml} 과 1:1 대응한다.
 */
public interface CommentMapper {

    /** 삭제 권한(작성자 본인 여부) 확인용. */
    Comment findById(@Param("id") Long id);

    /** {@code GET /mission-logs/{id}/comments} 용. 작성자 닉네임을 조인해 작성 순으로 돌려준다. */
    List<CommentView> findViewsByMissionLogId(@Param("missionLogId") Long missionLogId);

    int insert(Comment comment);

    int deleteById(@Param("id") Long id);
}
