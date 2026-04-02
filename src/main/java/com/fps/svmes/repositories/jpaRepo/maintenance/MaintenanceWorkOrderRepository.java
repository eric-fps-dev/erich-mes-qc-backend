//package com.fps.svmes.repositories.jpaRepo.maintenance;
//
//import com.fps.svmes.models.sql.maintenance.MaintenanceWorkOrder;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//
//public interface MaintenanceWorkOrderRepository extends JpaRepository<MaintenanceWorkOrder, Integer> {
//    List<MaintenanceWorkOrder> findByStatus(int i);
//    MaintenanceWorkOrder findByIdAndStatus(int id, int status);
//}
