package com.marudham.web.controller;
import com.marudham.web.model.*;
import com.marudham.web.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;
@RestController @RequestMapping("/api")
public class ApiController {
 private final CollectionPointRepository points; private final CollectionRequestRepository requests;
 public ApiController(CollectionPointRepository p,CollectionRequestRepository r){points=p;requests=r;}
 @GetMapping("/points") public List<CollectionPoint> allPoints(){return points.findAll();}
 @PostMapping("/points") public CollectionPoint addPoint(@RequestBody Map<String,Object> body){
  String id=String.valueOf(body.getOrDefault("id", "")).trim(), name=String.valueOf(body.getOrDefault("name", "")).trim(), zone=String.valueOf(body.getOrDefault("zone", "General")).trim();
  double kg=number(body.get("wasteKg")); if(id.isEmpty()||name.isEmpty()||zone.isEmpty()||kg<0||kg>5000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter an ID, name, zone and waste from 0 to 5000 kg.");
  if(points.existsById(id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"That collection point ID already exists.");
  return points.save(new CollectionPoint(id,name,zone,kg));
 }
 @PutMapping("/points/{id}/waste") public CollectionPoint updateWaste(@PathVariable String id,@RequestBody Map<String,Object> b){CollectionPoint p=points.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Point not found"));double kg=number(b.get("wasteKg"));if(kg<0||kg>5000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Waste must be 0–5000 kg");p.setWasteKg(kg);return points.save(p);}
 @GetMapping("/requests") public List<CollectionRequest> allRequests(){return requests.findAllByOrderByCreatedAtDesc();}
 @PostMapping("/requests") public CollectionRequest createRequest(@RequestBody Map<String,String> b){String pointId=b.getOrDefault("pointId","");String priority=b.getOrDefault("priority","MEDIUM").toUpperCase();if(!Set.of("HIGH","MEDIUM","LOW").contains(priority))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Priority must be High, Medium or Low");CollectionPoint p=points.findById(pointId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Select a valid collection point"));if(p.getWasteKg()<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This point has no waste to collect");boolean open=requests.findAll().stream().anyMatch(r->r.getPointId().equals(pointId)&&(r.getStatus().equals("PENDING")||r.getStatus().equals("IN_PROGRESS")));if(open)throw new ResponseStatusException(HttpStatus.CONFLICT,"This point already has an open request");return requests.save(new CollectionRequest(p.getId(),p.getName(),priority,p.getWasteKg()));}
 @PostMapping("/requests/{id}/complete") public CollectionRequest complete(@PathVariable Long id){CollectionRequest r=requests.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Request not found"));if(!r.getStatus().equals("PENDING"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only pending requests can be completed");CollectionPoint p=points.findById(r.getPointId()).orElseThrow();p.setWasteKg(Math.max(0,p.getWasteKg()-r.getWasteKg()));points.save(p);r.setStatus("COMPLETED");return requests.save(r);}
 @GetMapping("/dashboard") public Map<String,Object> dashboard(){List<CollectionPoint> ps=points.findAll();List<CollectionRequest> rs=requests.findAll();Map<String,Object> m=new LinkedHashMap<>();m.put("pointCount",ps.size());m.put("totalWasteKg",ps.stream().mapToDouble(CollectionPoint::getWasteKg).sum());m.put("pendingCount",rs.stream().filter(r->r.getStatus().equals("PENDING")).count());m.put("completedCount",rs.stream().filter(r->r.getStatus().equals("COMPLETED")).count());m.put("highPriorityCount",rs.stream().filter(r->r.getPriority().equals("HIGH")&&r.getStatus().equals("PENDING")).count());return m;}
 private double number(Object v){try{return Double.parseDouble(String.valueOf(v));}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Waste kg must be a number");}}
}
