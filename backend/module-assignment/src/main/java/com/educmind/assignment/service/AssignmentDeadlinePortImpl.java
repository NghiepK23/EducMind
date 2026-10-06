package com.educmind.assignment.service;

import com.educmind.assignment.repository.AssignmentRepository;
import com.educmind.assignment.repository.AssignmentSubmissionRepository;
import com.educmind.common.port.AssignmentDeadlinePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AssignmentDeadlinePortImpl implements AssignmentDeadlinePort {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public AssignmentDeadlinePortImpl(AssignmentRepository assignmentRepository,
                                      AssignmentSubmissionRepository submissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DueAssignment> findPublishedDueBetween(OffsetDateTime from, OffsetDateTime to) {
        return assignmentRepository.findPublishedDueBetween(from, to).stream()
                .map(a -> new DueAssignment(a.getId(), a.getCourseId(), a.getTitle(), a.getDueDate()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findSubmittedStudentIds(UUID assignmentId) {
        return new HashSet<>(submissionRepository.findSubmittedStudentIds(assignmentId));
    }
}
