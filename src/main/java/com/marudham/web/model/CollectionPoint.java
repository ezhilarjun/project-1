package com.marudham.web.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="collection_points")
public class CollectionPoint {
 @Id @Column(length=20) private String id;
 @Column(nullable=false) private String name;
 @Column(nullable=false) private String zone;
 @Column(nullable=false) private double wasteKg;
 @Column(nullable=false) private LocalDateTime updatedAt = LocalDateTime.now();
 public CollectionPoint() {}
 public CollectionPoint(String id,String name,String zone,double wasteKg){this.id=id;this.name=name;this.zone=zone;this.wasteKg=wasteKg;this.updatedAt=LocalDateTime.now();}
 @PreUpdate public void touch(){updatedAt=LocalDateTime.now();}
 public String getId(){return id;} public void setId(String v){id=v;}
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getZone(){return zone;} public void setZone(String v){zone=v;}
 public double getWasteKg(){return wasteKg;} public void setWasteKg(double v){wasteKg=v;}
 public LocalDateTime getUpdatedAt(){return updatedAt;}
}
