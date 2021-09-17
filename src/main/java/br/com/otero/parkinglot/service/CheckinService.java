package br.com.otero.parkinglot.service;

import br.com.otero.parkinglot.exception.BusinessException;
import br.com.otero.parkinglot.exception.NotFoundException;
import br.com.otero.parkinglot.models.Carro;
import br.com.otero.parkinglot.models.CheckIn;
import br.com.otero.parkinglot.models.dto.CheckinRequest;
import br.com.otero.parkinglot.models.dto.CheckinResponse;
import br.com.otero.parkinglot.models.dto.HistoricoCheckInResponse;
import br.com.otero.parkinglot.repository.CheckinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CheckinService {

    @Autowired
    CheckinRepository checkinRepository;


    public List<HistoricoCheckInResponse> obterhistorico(Date dataEntrada, Date dataSaida) {

        List<CheckIn> checkIns = this.checkinRepository.FindByPeriodo(dataEntrada, dataSaida);
        return checkIns.stream().map(checkIn -> {
            String tempo = (((checkIn.getDataSaida().getTime() - checkIn.getDataEntrada().getTime()) / 1000) / 60) + " minutos";

            return HistoricoCheckInResponse.builder()
                    .id(checkIn.getId())
                    .periodo(tempo)
                    .pago(checkIn.getPagamento())
                    .checkout(checkIn.getDataSaida() != null)
                    .build();
        }).collect(Collectors.toList());

    }

    public CheckinResponse newCheckin(CheckinRequest newCheckin) {
        Carro carro = Carro.builder()
                .brand(newCheckin.getBrand())
                .model(newCheckin.getModel())
                .plate(newCheckin.getPlate()).build();

        CheckIn checkIn = CheckIn.builder()
                .dataEntrada(new Date())
                .carro(carro)
                .pagamento(false)
                .build();


        if (newCheckin.getPlate() == null) {
            throw new BusinessException("Placa invalida");
        }
        if (newCheckin.getPlate().length() != 7) {
            throw new BusinessException("Placa deve conter 7 digitos");
        }
        if (newCheckin.getModel().isEmpty() || newCheckin.getModel().isBlank()) {
            throw new BusinessException("Modelo invalido");
        }
        if (newCheckin.getBrand().isEmpty() || newCheckin.getBrand().isBlank()) {
            throw new BusinessException("Montadora invalida");
        }


        this.checkinRepository.save(checkIn);

        return CheckinResponse.builder()
                .id(checkIn.getId())
                .build();

    }

    public void checkOut(Long id) {
        Optional<CheckIn> checkout = Optional.ofNullable(this.checkinRepository.findById(id).orElseThrow(() -> new NotFoundException("Parking id " + id + " not found")));
//        if (checkout.get().getPagamento() == false) {
//            throw new BusinessException("Pagamento não realizado, pagar por favor!");
//        }

        checkout.get().setDataSaida(new Date());

        this.checkinRepository.save(checkout.get());
    }

    public void pagamento(Long id) {
        Optional<CheckIn> checkout = this.checkinRepository.findById(id);

        if (checkout.isEmpty()) {
            throw new BusinessException("CheckIn inexistente");
        }

        if (checkout.get().getPagamento()) {
            throw new BusinessException("Pagamento já realizado");

        }
        checkout.get().setPagamento(true);

        this.checkinRepository.save(checkout.get());
    }
}
