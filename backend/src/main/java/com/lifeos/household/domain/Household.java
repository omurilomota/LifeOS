package com.lifeos.household.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "households")
public class Household extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    protected Household() {}

    public Household(String name, UUID ownerUserId) {
        this.name = name;
        this.ownerUserId = ownerUserId;
    }

    public String getName() { return name; }
    public UUID getOwnerUserId() { return ownerUserId; }
}
