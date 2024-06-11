package com.asidG1.orchestratorservice.service;


import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.asidG1.orchestratorservice.feignclient.ParentClient;
import com.asidG1.orchestratorservice.feignclient.StudentClient;
import com.asidG1.orchestratorservice.feignclient.TownClient;
import com.asidG1.orchestratorservice.model.DTOs.ParentDTO;
import com.asidG1.orchestratorservice.model.DTOs.ParentRegisterDTO;
import com.asidG1.orchestratorservice.model.DTOs.StudentDTO;
import com.asidG1.orchestratorservice.model.DTOs.StudentRegisterDTO;
import com.asidG1.orchestratorservice.model.DTOs.TownDTO;

import feign.FeignException;

@Service
public class StudentService {

    private final ModelMapper mapper;

    @Autowired
    private ParentClient parentClient;

    @Autowired
    private StudentClient studentClient;

    @Autowired
    private TownClient townClient;

    @Autowired
    public StudentService(ModelMapper mapper) {
        this.mapper = mapper;
    }

    public ResponseEntity<TownDTO> registarCidade(TownDTO townDTO){
        try {
            ResponseEntity<TownDTO> existingTownResponse = townClient.getTownByName(townDTO.getName());
            HttpHeaders headers = new HttpHeaders();
            headers.addAll(existingTownResponse.getHeaders());
            headers.add("cidadeCriada", "false");
            return ResponseEntity.status(existingTownResponse.getStatusCode())
                             .headers(headers)
                             .body(existingTownResponse.getBody());
        } catch (FeignException e) {
            try{
                ResponseEntity<TownDTO> createdTownResponse = townClient.addTown(townDTO);
                HttpHeaders headers = new HttpHeaders();
                headers.addAll(createdTownResponse.getHeaders());
                headers.add("cidadeCriada", "true");
                return ResponseEntity.status(createdTownResponse.getStatusCode())
                                    .headers(headers)
                                    .body(createdTownResponse.getBody());
            }catch(FeignException ee){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }
    }

    public ResponseEntity<ParentRegisterDTO> registarPai(ParentDTO parentDTO) {
        // Separar cidade
        TownDTO townParent = parentDTO.getTown();

        //Criar a cidade
         ResponseEntity<TownDTO> responseTown = registarCidade(townParent);

         if(responseTown.getStatusCode().is5xxServerError() || responseTown.getStatusCode().is4xxClientError()){
             System.out.println("ORCHESTRATOR-SERVICE: Operação cancelada! Erro ao registar cidade do Pai");
             System.out.println(responseTown.getStatusCode());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }

         Boolean cidadeCriada = Boolean.parseBoolean(responseTown.getHeaders().get("cidadeCriada").get(0));
         System.out.println("TOWN-SERVICE: Cidade do Pai registada com sucesso");

        ParentRegisterDTO parentRegister = new ParentRegisterDTO();
        parentRegister.setAge(parentDTO.getAge());
        parentRegister.setEGN(parentDTO.getEGN());
        parentRegister.setEmail(parentDTO.getEmail());
        parentRegister.setFirstName(parentDTO.getFirstName());
        parentRegister.setGender(parentDTO.getGender());
        parentRegister.setId(parentDTO.getId());
        parentRegister.setLastName(parentDTO.getLastName());
        parentRegister.setMiddleName(parentDTO.getMiddleName());
        parentRegister.setPhoneNumber(parentDTO.getPhoneNumber());
        parentRegister.setTown(responseTown.getBody().getId());

        // Usar API ParentService para registar o pai, se der erro, apaga a cidade se foi criada

        ResponseEntity<ParentRegisterDTO> response = null;
        try{
            response = parentClient.addParent(parentRegister);
            HttpHeaders headers = new HttpHeaders();
             headers.addAll(response.getHeaders());
            headers.add("cidadeCriada", cidadeCriada.toString());
            return ResponseEntity.status(response.getStatusCode())
                                .headers(headers)
                                .body(response.getBody());
        }catch (FeignException e){
            if(cidadeCriada == true){
                ResponseEntity<TownDTO> responseTownDel = townClient.deleteTown(responseTown.getBody().getId());
                System.out.println("TOWN-SERVICE: Cidade do pai eliminada! Erro ao registar Pai.");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Registar estudante com o pai
    public ResponseEntity<?> registarEstudante(StudentDTO studentDTO) {

        // Se existir Parent, separar entre StudentDTO e ParentDTO
        ParentDTO parentDTO = studentDTO.getParent();

        //Criar o pai, se der erro, apagar a cidade
        ResponseEntity<ParentRegisterDTO> responsePai = registarPai(parentDTO);

        if(responsePai.getStatusCode().is5xxServerError() || responsePai.getStatusCode().is4xxClientError()){
            System.out.println("ORCHESTRATOR-SERVICE: Operação cancelada! Erro ao registar Pai");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        ParentRegisterDTO parentToMap = responsePai.getBody();
        System.out.println("PARENT-SERVICE: Pai registado com sucesso");
        Boolean cidadePaiCriada = Boolean.parseBoolean(responsePai.getHeaders().get("cidadeCriada").get(0));
        //System.out.println("Cidade pai criada: " + cidadePaiCriada);


        TownDTO studentTown = studentDTO.getTown(); 
        ResponseEntity<TownDTO> responseTown = registarCidade(studentTown);

        if(responseTown.getStatusCode().is5xxServerError() || responseTown.getStatusCode().is4xxClientError()){
            System.out.println("ORCHESTRATOR-SERVICE: Operação cancelada! Erro ao registar cidade do Estudante");
            System.out.println(responseTown.getStatusCode());
            ResponseEntity<ParentRegisterDTO> responseParentDel = parentClient.deleteParentById(parentToMap.getId());
            System.out.println("PARENT-SERVICE: Pai eliminado! Erro ao registar cidade do Estudante");
            if(cidadePaiCriada){
                ResponseEntity<TownDTO> responseTownDel = townClient.deleteTown(parentToMap.getTown());
                System.out.println("TOWN-SERVICE: Cidade do Pai eliminada! Erro ao registar cidade do Estudante");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        Boolean cidadeEstudanteCriada = Boolean.parseBoolean(responseTown.getHeaders().get("cidadeCriada").get(0));
        System.out.println("TOWN-SERVICE: Cidade do estudante registada com sucesso");

        // Se não, usar API StudentService para registar aluno
        StudentRegisterDTO student = new StudentRegisterDTO();
        student.setAge(studentDTO.getAge());
        student.setEGN(studentDTO.getEGN());
        student.setEmail(studentDTO.getEmail());
        student.setFirstName(studentDTO.getEmail());
        student.setGender(studentDTO.getGender());
        student.setId(studentDTO.getId());
        student.setLastName(studentDTO.getLastName());
        student.setMiddleName(studentDTO.getMiddleName());
        student.setParent(parentToMap.getId());
        student.setTown(responseTown.getBody().getId());

        ResponseEntity<StudentRegisterDTO> responseStudent = null;
        try{
            responseStudent = studentClient.addStudent(student);
            System.out.println("STUDENT-SERVICE: Estudante registado com sucesso");
            return responseStudent;
        }catch (FeignException e){
            System.out.println("ORCHESTRATOR-SERVICE: Operação cancelada! Erro ao registar estudante");
            if(cidadeEstudanteCriada == true){
                ResponseEntity<TownDTO> responseTownDel = townClient.deleteTown(responseTown.getBody().getId());
                System.out.println("TOWN-SERVICE: Cidade do Estudante eliminada! Erro ao registar Estudante");
            }
            ResponseEntity<ParentRegisterDTO> responseParentDel = parentClient.deleteParentById(parentToMap.getId());
            System.out.println("PARENT-SERVICE: Pai eliminado! Erro ao registar Estudante");
            if(cidadePaiCriada){
                ResponseEntity<TownDTO> responseTownDel = townClient.deleteTown(parentToMap.getTown());
                System.out.println("TOWN-SERVICE: Cidade do Pai eliminada! Erro ao registar Estudante");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao registar estudante com Pai e Cidade associado!");
        }

    }
}
