package pt.isep.escala.escalaapi.risk;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk")
class RiskController {

	private final RiskQuery risks;

	RiskController(RiskQuery risks) {
		this.risks = risks;
	}

	@GetMapping("/today")
	TodayRisk today() {
		return risks.today();
	}
}
