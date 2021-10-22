package cbtModel;

import uchicago.src.sim.engine.SimInit;

public class AlcoholBatchModel extends AlcoholModel {
	
	public static void main(String[] args) {
		SimInit init = new SimInit();
		AlcoholBatchModel model = new AlcoholBatchModel();
		init.loadModel(model, null, false);
		
		//////////////// FOR CALIBRATION
		// init.loadModel(model, "./params_baselineagent.txt", true);	// check calibration of agent characteristics
		// init.loadModel(model, "./params_baselinehood.txt",  true);	// check calibration of agent characteristics within neighborhoods
		// init.loadModel(model, "./params_mortality.txt",  true);		// check calibration of agent mortality over time
		// init.loadModel(model, "./params_baseline_substanceuse.txt",  true);		// check calibration of agent substance use status at baseline
		// init.loadModel(model, "./params_annual.txt",  true);	// check calibration of drinking status and violence over time
		// init.loadModel(model, "./params_homicide_calibration.txt",  true);	// check calibration of drinking status and violence over time
		// init.loadModel(model, "./params_hoods_afterburnin.txt",  true);	// check calibration of neighborhood conditions after burn-in period
		// init.loadModel(model, "./params_endhood.txt",  true);	// check calibration of agent characteristics within neighborhoods
		// init.loadModel(model, "./params_baseline_bevtype.txt",  true); // check calibration of beverage type		
		 init.loadModel(model,  "C:\\Users\\ashev\\Documents\\alcoholABM\\baseline_params1.txt",  true);
		// init.loadModel(model,  "C:\\Users\\ashev\\Documents\\alcoholABM\\reduce_outlets10.txt",  true);
		
		//////////////// NO INTERVENTION
		// init.loadModel(model, "./params_check_intervention.txt", true);// results from model with no intervention
		// init.loadModel(model, "./params_check_univ_interrupt.txt",  true);
		// init.loadModel(model, "./params_check_target_interrupt.txt", true);
		// init.loadModel(model, "./params_check_noint.txt", true);		
		// init.loadModel(model, "./params_check_police_int_0increase.txt", true);
		
		//////////////// VIOLENCE INTERRUPTER INTERVENTION
		// init.loadModel(model, "./params_calib_univ_interrupt.txt",  true);
		// init.loadModel(model, "./params_calib_target_interrupt.txt", true);
		// init.loadModel(model, "./params_interrupt_1all.txt",  true);
				
		//////////////// DRINKING NORMS INTERVENTION
		// init.loadModel(model, "./params_norms_intervention_all30yr.txt",  true); 	// results from model with universal drinking norms interventions
				
		//////////////// ALCOHOL OUTLET UNIVERSAL INTERVENTIONS
		//init.loadModel(model, "./params_outlet_random.txt", true);	// close randomly selected outlets
		// init.loadModel(model, "./params_outlet_intervention_all5yr.txt", true);	// results from model with universal alcohol outlet intervention for 5 yrs
		// init.loadModel(model, "./params_outlet_intervention_all30yr.txt", true);	// results from model with universal alcohol outlet intervention for 30 yrs
						
		//////////////// ALCOHOL OUTLET TARGETED INTERVENTIONS
		// init.loadModel(model, "./params_outlet_intervention_target30yr.txt", true);	// results from model with targeted alcohol outlet intervention for 30 yrs
		
		/////////////// COMMUNITY POLICING UNIVERSAL INTERVENTIONS
		// init.loadModel(model, "./params_calib_univ_policing.txt",  true);
		// init.loadModel(model, "./params_police_0change5yr.txt",  true);
		// init.loadModel(model, "./params_police_0change10yr.txt",  true);
		// init.loadModel(model, "./params_police_15change5yr.txt",  true);
		// init.loadModel(model, "./params_police_15change10yr.txt",  true);
		
		//////////////// ALCOHOL OUTLETS CLOSING EARLY
		// init.loadModel(model,  "./params_outlet_early_all.txt",  true);
		// init.loadModel(model,  "./params_outlet_early_target.txt",  true);
		
		/////////////// ALCOHOL TAXATION
		// init.loadModel(model,  "./params_taxation_universal.txt",  true);
		// init.loadModel(model,  "C:\\Users\\ashev\\Documents\\alcoholABM\\params_taxation_beer10.txt",  true);
		
		// init.loadModel(model,  "./params_policing_intervention_all1yr.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all5yr.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all30yr.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all1yr_80.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all5yr_80.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all30yr_80.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all1yr_65.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all5yr_65.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all30yr_65.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all1yr_50.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all5yr_50.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_all30yr_50.txt", true);		// results from model with universal policing intervention for 1 yr
								
		/////////////// COMMUNITY POLICING TARGETED INTERVENTIONS
		// init.loadModel(model,  "./params_policing_intervention_target1yr.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_target5yr.txt", true);		// results from model with universal policing intervention for 5 yrs
		// init.loadModel(model,  "./params_policing_intervention_target30yr.txt", true);		// results from model with universal policing intervention for 30 yrs
		// init.loadModel(model,  "./params_policing_intervention_target1yr_85.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_target5yr_85.txt", true);		// results from model with universal policing intervention for 5 yr
		// init.loadModel(model,  "./params_policing_intervention_target30yr_85.txt", true);		// results from model with universal policing intervention for 30 yr
		// init.loadModel(model,  "./params_policing_intervention_target1yr_95.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_target5yr_95.txt", true);		// results from model with universal policing intervention for 5 yr
		// init.loadModel(model,  "./params_policing_intervention_target30yr_95.txt", true);		// results from model with universal policing intervention for 30 yr
		// init.loadModel(model,  "./params_policing_intervention_target1yr_50.txt", true);		// results from model with universal policing intervention for 1 yr
		// init.loadModel(model,  "./params_policing_intervention_target5yr_50.txt", true);		// results from model with universal policing intervention for 5 yr
		// init.loadModel(model,  "./params_policing_intervention_target30yr_50.txt", true);		// results from model with universal policing intervention for 30 yr

		////////////// SENSITIVITY ANALYSES
		
		//////////////////////////////////////////////////////////// #1: neighborhood influence
		// no intervention
		// init.loadModel(model,  "./params_sens1_noint.txt", true);		
		// univeral cbt intervention
		// init.loadModel(model, "./params_sens1_cbtall.txt", true);
		// targeted cbt intervention
		// init.loadModel(model, "./params_sens1_cbttarget.txt", true);
		// universal outlet intervention
		// init.loadModel(model,  "./params_sens1_outletall.txt", true);
		// targeted outlet intervention
		// init.loadModel(model,  "./params_sens1_outlettarget.txt", true);
		// universal policing intervention
		// init.loadModel(model, "./params_sens1_policingall.txt", true);
		// init.loadModel(model, "./params_sens1_policingall_1year.txt", true);
		// targeted policing intervention
		// init.loadModel(model, "./params_sens1_policingtarget.txt", true);		
		// init.loadModel(model, "./params_sens1_policingtarget_1year.txt", true);	
		
		//////////////////////////////////////////////////////////// #2: proximity of victims and perpetrators
		// no intervention
		// init.loadModel(model,  "./params_sens2_noint.txt", true);		
		// univeral cbt intervention
		// init.loadModel(model, "./params_sens2_cbtall.txt", true);
		// targeted cbt intervention
		// init.loadModel(model, "./params_sens2_cbttarget.txt", true);
		// universal outlet intervention
		// init.loadModel(model,  "./params_sens2_outletall.txt", true);
		// targeted outlet intervention
		// init.loadModel(model,  "./params_sens2_outlettarget.txt", true);
		// universal policing intervention
		// init.loadModel(model, "./params_sens2_policingall.txt", true);
		// init.loadModel(model, "./params_sens2_policingall_1year.txt", true);
		// targeted policing intervention
		// init.loadModel(model, "./params_sens2_policingtarget.txt", true);		
		// init.loadModel(model, "./params_sens2_policingtarget_1year.txt", true);	
		
		//////////////////////////////////////////////////////////// #3: proximity of witnesses
		// no intervention
		// init.loadModel(model,  "./params_sens3_noint.txt", true);		
		// univeral cbt intervention
		// init.loadModel(model, "./params_sens3_cbtall.txt", true);
		// targeted cbt intervention
		// init.loadModel(model, "./params_sens3_cbttarget.txt", true);
		// universal outlet intervention
		// init.loadModel(model,  "./params_sens3_outletall.txt", true);
		// targeted outlet intervention
		// init.loadModel(model,  "./params_sens3_outlettarget.txt", true);
		// universal policing intervention
		// init.loadModel(model, "./params_sens3_policingall.txt", true);
		// init.loadModel(model, "./params_sens3_policingall_1year.txt", true);
		// targeted policing intervention
		// init.loadModel(model, "./params_sens3_policingtarget.txt", true);		
		// init.loadModel(model, "./params_sens3_policingtarget_1year.txt", true);	
		
	}
	
	public void setup() {
		super.setup();
	}
	
	public void buildModel() {
		super.buildModel();
	}
	
	public void buildSchedule() {
		super.buildSchedule();
	}

} // end of AlcoholBatchModel
