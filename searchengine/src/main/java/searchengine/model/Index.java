package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "`index`")
@Getter
@Setter
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "page_id")
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id")
    private Lemma lemma;

    @Column(name = "`rank`", columnDefinition = "FLOAT", nullable = false)
    private Double rank;
}
