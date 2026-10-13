package com.marudham.web.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="collection_requests")
public class CollectionRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String pointId;
 @Column(nullable=false) private String pointName;
 @Column(nullable=false) private String priority;
 @Column(nullable=false) private String status="PENDING";
 @Column(nullable=false) private double wasteKg;
 @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
 public CollectionRequest(){}
 public CollectionRequest(String pointId,String pointName,String priority,double wasteKg){this.pointId=pointId;this.pointName=pointName;this.priority=priority;this.wasteKg=wasteKg;this.status="PENDING";this.createdAt=LocalDateTime.now();}
 public Long getId(){return id;} public String getPointId(){return pointId;} public String getPointName(){return pointName;} public String getPriority(){return priority;} public String getStatus(){return status;} public double getWasteKg(){return wasteKg;} public LocalDateTime getCreatedAt(){return createdAt;} public void setStatus(String s){status=s;}
}
