package com.tjoeun.boxmon.feature.user.domain;

import com.tjoeun.boxmon.feature.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Shipper {
    @Id
    private Long shipperId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "shipper_id", nullable = false)
    private User user;

}
