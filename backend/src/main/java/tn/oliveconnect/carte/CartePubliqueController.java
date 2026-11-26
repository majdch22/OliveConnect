package tn.oliveconnect.carte;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.carte.CartePubliqueService.VergerPublic;

/**
 * GET /api/public/vergers : carte publique des vergers (visiteurs, sans connexion).
 * Ce qui est montré, et ce qui est caché pour respecter la vie privée, est décrit dans {@link CartePubliqueService}.
 */
@RestController
@RequiredArgsConstructor
public class CartePubliqueController {

    private final CartePubliqueService cartePubliqueService;

    @GetMapping("/api/public/vergers")
    public List<VergerPublic> vergers() {
        return cartePubliqueService.vergers();
    }
}
