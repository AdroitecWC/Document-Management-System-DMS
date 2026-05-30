package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DocumentLifeCycleState")
public class DocumentLifeCycleState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", nullable = false)
    private LifeCycleState state;              // ← correct class name

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "comments")
    private String comments;

    // No-arg constructor (JPA required)
    public DocumentLifeCycleState() {}

    // Getters and Setters
    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }
    public Long getDocumentId()                     { return documentId; }
    public void setDocumentId(Long documentId)      { this.documentId = documentId; }
    public LifeCycleState getState()                { return state; }
    public void setState(LifeCycleState state)       { this.state = state; }
    public LocalDateTime getChangedAt()             { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt){ this.changedAt = changedAt; }
    public Long getChangedBy()                      { return changedBy; }
    public void setChangedBy(Long changedBy)        { this.changedBy = changedBy; }
    public String getComments()                     { return comments; }
    public void setComments(String comments)        { this.comments = comments; }

    // Builder pattern (used in assignWorkflow)
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final DocumentLifeCycleState obj = new DocumentLifeCycleState();
        public Builder documentId(Long v)       { obj.documentId = v; return this; }
        public Builder state(LifeCycleState v)  { obj.state = v; return this; }
        public Builder changedAt(LocalDateTime v){ obj.changedAt = v; return this; }
        public Builder changedBy(Long v)        { obj.changedBy = v; return this; }
        public Builder comments(String v)       { obj.comments = v; return this; }
        public DocumentLifeCycleState build()   { return obj; }
    }
}