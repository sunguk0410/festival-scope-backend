package likelion.festivalscope.parking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Builder @Entity @Table(name = "parking")
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Parking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parking_id") private Long id;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal latitude;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal longitude;
    @Column(nullable = false) private Integer capacity;
}
