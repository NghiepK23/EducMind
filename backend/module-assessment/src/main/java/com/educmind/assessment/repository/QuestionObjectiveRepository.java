package com.educmind.assessment.repository;

import com.educmind.assessment.entity.QuestionObjective;
import com.educmind.assessment.entity.QuestionObjectiveId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionObjectiveRepository extends JpaRepository<QuestionObjective, QuestionObjectiveId> {

    List<QuestionObjective> findByIdQuestionId(UUID questionId);

    List<QuestionObjective> findByIdQuestionIdIn(List<UUID> questionIds);

    void deleteByIdQuestionId(UUID questionId);

    boolean existsByIdQuestionIdAndIdObjectiveId(UUID questionId, UUID objectiveId);
}
