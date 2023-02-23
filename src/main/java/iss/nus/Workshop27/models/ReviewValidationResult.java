package iss.nus.Workshop27.models;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

public class ReviewValidationResult {
    
    private Integer status;
    private LocalDateTime timestamp = LocalDateTime.now();
    private List<String> errors;

    public ReviewValidationResult(){
    }

    public ReviewValidationResult(Integer status, List<String> errors){
        this.status = status;
        this.errors = errors;
    }

    public JsonObject toJson() {

        JsonArrayBuilder errArr = Json.createArrayBuilder();
        errors.forEach(x -> errArr.add(x)); 

        return Json.createObjectBuilder()
                .add("status", this.getStatus())
                .add("timestamp", this.getTimestamp().toString())
                .add("errors", errArr)
                .build();
    }

    @Override
    public String toString() {
        return "ReviewValidationResult [status=" + status + ", timestamp=" + timestamp + ", errors=" + errors + "]";
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    

    
}
