package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.MetadataEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetadataEnumRepository extends JpaRepository<MetadataEnum, Long> {

    /**
     * Find all enum values for a specific metadata definition
     */
    List<MetadataEnum> findByMetadataDefinitionMetadataId(Long metadataId);

    /**
     * Check if an enum value already exists for a metadata definition
     */
    boolean existsByMetadataDefinitionMetadataIdAndValue(Long metadataId, String value);

    /**
     * Delete all enum values for a metadata definition
     */
    void deleteByMetadataDefinitionMetadataId(Long metadataId);
}
