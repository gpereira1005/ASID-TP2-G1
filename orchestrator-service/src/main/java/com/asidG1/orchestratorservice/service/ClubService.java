package com.asidG1.orchestratorservice.service;

import com.asidG1.orchestratorservice.model.DTOs.ClubDTO;
import com.asidG1.orchestratorservice.model.DTOs.StudentRegisterDTO;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.asidG1.orchestratorservice.feignclient.ClubClient;
import com.asidG1.orchestratorservice.feignclient.StudentClient;

import feign.FeignException;

@Service
public class ClubService {
    private final ModelMapper mapper;

    @Autowired
    private StudentClient studentClient; 

    @Autowired
    private ClubClient clubClient;

    @Autowired
    public ClubService(ModelMapper mapper) {
        this.mapper = mapper;
    }

    public ResponseEntity<?> addStudentToClub(Long studentID, Long clubID) {
        boolean addedToClub = false;
        try{
            ResponseEntity<ClubDTO> responseClub = clubClient.addStudentToClub(clubID, studentID);
            addedToClub = true;
            System.out.println("CLUB-SERVICE: Estudante associado ao clube");
            ResponseEntity<StudentRegisterDTO> responseStudent = studentClient.addClubToStudent(clubID, studentID);
            System.out.println("STUDENT-SERVICE: Club associado ao estudante");
            return ResponseEntity.ok(responseClub.getBody());
        }catch(FeignException e){
            if(addedToClub){
                ResponseEntity<ClubDTO> responseClub = clubClient.removeStudentToClub(clubID, studentID);
                System.out.println("CLUB-SERVICE: Estudante removido! Student-Service não está disponivel");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao associar o estudante ao club");
        }

    
    }
}
