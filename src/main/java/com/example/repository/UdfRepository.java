//package com.example.repository;
//
//import com.example.model.Udf;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface UdfRepository extends JpaRepository<Udf, String> {
//
//    List<Udf> findByTeam(String team);
//
//    List<Udf> findByType(Udf.UdfType type);
//
//    List<Udf> findByIsActive(Boolean isActive);
//
//    List<Udf> findByIsApproved(Boolean isApproved);
//
//    Optional<Udf> findByName(String name);
//}
