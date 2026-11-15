package pt.isep.escala.escalaapi.piquete;

enum SlotStatus {
	OPEN,
	PENDING,
	ELIGIBLE,
	PROPOSED,
	FILLED,
	/** The evaluation could not be made or applied (RF24); an operator resolves it by replaying the DLQ (SCRUM-95). */
	EVALUATION_FAILED
}
