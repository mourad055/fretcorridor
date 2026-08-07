package com.flysoft.fretcorridor.ida.repository;

import com.flysoft.fretcorridor.ida.entity.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {
}
