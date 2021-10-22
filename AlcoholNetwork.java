/*
 * Alcohol ABM
 *
 *     This model will compare interventions aimed at reducing racial disparities in alcohol-related
 * homicide, using New York City as the place and population of interest.
 *
 *     The network class creates the social network linking agents to each other. Linked agents influence
 * each other's drinking behaviors.
 * 
 * Revised May 27, 2014
 * 
 * Revisions: (1) Add consideration of drinking status when forming social network - 5/27/2014
 * 
 */

package cbtModel;

import java.util.ArrayList;
import uchicago.src.sim.util.Random;
import java.util.Iterator;

public class AlcoholNetwork {

	// Start with zero edges
	public static int	numEdges = 0;
	public int numNodes;
	
	// Keep track of the degree of each node
	ArrayList<Integer> socialrelationships = new ArrayList<Integer>();
	
	// the Alcohol Social Network Constructor
	public AlcoholNetwork() {
		
		// create lists of connected agents;
		SocialNetworkPopulate();
	
	} // end of Alcohol Social Network constructor
	
	public void SocialNetworkPopulate() {
		System.out.println("Populate Social Network Method called");
		
		// Outer loop i is the node currently being attached to the network
		Iterator<AlcoholAgent> iter = AlcoholModel.SocialNetworkList.iterator();
		while(iter.hasNext()) {
			
			// "a" is the node being added
			AlcoholAgent a = iter.next();
			
			// Cycle through "while" loop as long as agent has less than allotted friends
			// NOTE: attempt to find target friends for 10,000 iterations, then give up
			int outCycleInt = 0;
			while (a.getTotalFriends() < a.finalfriendsize && outCycleInt < 10000) {
				
				outCycleInt++;
				
				// Begin match algorithm
				// FIRST CHOICE: Spatial or demographics match
				double space_demo_choice = Random.uniform.nextDoubleFromTo(0, 1);
				
				int cycle = 0;
				// Spatial match algorithm implemented 25% of the time
				if (space_demo_choice <= 0.25) {
					while (cycle < 10000) {
						
						// Select an agent at random for a potential match
						int nodestart = 0 + (int)(Math.random() * ((AlcoholModel.SocialNetworkList.size()-1 - 0) + 1));
						// "b" is the agent under evaluation as a potential match
						AlcoholAgent b = (AlcoholAgent)AlcoholModel.SocialNetworkList.get(nodestart);
						
						// Check if (1) trying to add itself, (2) add to maxed-out node, (3) add to a node that has already been added
						// If so, do nothing
						if (a.getID()==b.getID() || (b.getTotalFriends()>b.finalfriendsize) || a.isFriend(b)) {
							// do nothing
						} 
						else {
							int xA = a.getX();
							int yA = a.getY();
							int xB = b.getX();
							int yB = b.getY();
							
							if (Math.abs(xA-xB) <= 100 && Math.abs(yA-yB) <= 100) {
								boolean Match1 = true, Match2 = true, Match3 = true, Match4 = true, Match5 = true;;
							
								// Age check
								double age_choice = Random.uniform.nextDoubleFromTo(0, 1);
								double Age_A = a.getAge();
								double Age_B = b.getAge();
								if (age_choice > 0.185) {
									if (Math.abs(Age_A-Age_B)<=10) { Match1 = true; } else { Match1 = false; }
								}
								
								// Gender check
								double gender_choice = Random.uniform.nextDoubleFromTo(0, 1);
								int Gender_A = a.getGender();
								int Gender_B = b.getGender();
								if (gender_choice > 0.995) {
									if (Gender_A == Gender_B) { Match2 = true; } else { Match2 = false; }
								}
								
								// Race check
								double race_choice = Random.uniform.nextDoubleFromTo(0, 1);
								int Race_A = a.getRace();
								int Race_B = b.getRace();
								if (race_choice > 0.01) {
									if (Race_A == Race_B) { Match3 = true; } else { Match3 = false; }
								}
								
								// Education check
								double education_choice = Random.uniform.nextDoubleFromTo(0, 1);
								int Edu_A = a.getEducation();
								int Edu_B = b.getEducation();
								if (education_choice > 0.25) {
									if (Edu_A == Edu_B) { Match4 = true; } else { Match4 = false; }
								}
								
								// Drinking status check
								double drinking_choice = Random.uniform.nextDoubleFromTo(0, 1);
								int Drk_A = a.getDrinkStat();
								int Drk_B = b.getDrinkStat();
								if (drinking_choice <= 0.15) {
									if (Drk_A == Drk_B) { Match5 = true; } else { Match5 = false; }
								}
								
								// Match node
								if (Match1 && Match2 && Match3 && Match4 && Match5) {
									b.setFriend(a);
									a.setFriend(b);
									numEdges+=1;
									break;
								}
							} // spatial loop
						} // end of else loop
						
						cycle += 1;
						
						} // end of while loop		
					} // end of spatial matching
			
					// Demographic match algorithm implemented 75% of the time
					cycle = 0;
					if (space_demo_choice > 0.25) {
						while (cycle < 10000) {
							
							// Select an agent at random for a potential match
							int nodestart = 0 + (int)(Math.random() * ((AlcoholModel.SocialNetworkList.size()-1 - 0) + 1));
							// "b" is the agent under evaluation as a potential match
							AlcoholAgent b = (AlcoholAgent)AlcoholModel.SocialNetworkList.get(nodestart);
							
							// Check if (1) trying to add itself, (2) add to maxed-out node, (3) add to a node that has already been added
							// If so, do nothing
							if (a.getID()==b.getID() || (b.getTotalFriends()>b.finalfriendsize) || a.isFriend(b)) {
								// do nothing
							} 
							else {
								boolean Match1 = true, Match2 = true, Match3 = true, Match4 = true, Match5 = true;
								
								// Age check
								double age_choice = Random.uniform.nextDoubleFromTo(0, 1);
								double Age_A = a.getAge();
								double Age_B = b.getAge();
								if (age_choice > 0.185) {
									if (Math.abs(Age_A-Age_B)<=10) { Match1 = true; } else { Match1 = false; }
								}
								
								// Gender check
								double gender_choice = Random.uniform.nextDoubleFromTo(0, 1);
								int Gender_A = a.getGender();
								int Gender_B = b.getGender();
								if (gender_choice > 0.995) {
									if (Gender_A == Gender_B) { Match2 = true; } else { Match2 = false; }
								}
								
								// Race check
								double race_choice = Random.uniform.nextDoubleFromTo(0, 1);
								int Race_A = a.getRace();
								int Race_B = b.getRace();
								if (race_choice > 0.01) {
									if (Race_A == Race_B) { Match3 = true; } else { Match3 = false; }
								}
								
								// Education check
								double education_choice = Random.uniform.nextDoubleFromTo(0, 1);
								int Edu_A = a.getEducation();
								int Edu_B = b.getEducation();
								if (education_choice > 0.25) {
									if (Edu_A == Edu_B) { Match4 = true; } else { Match4 = false; }
								}
								
								// Drinking status check
								double drinking_choice = Random.uniform.nextDoubleFromTo(0, 1);
								int Drk_A = a.getDrinkStat();
								int Drk_B = b.getDrinkStat();
								if (drinking_choice <= 0.15) {
									if (Drk_A == Drk_B) { Match5 = true; } else { Match5 = false; }
								}
								
								// Match node
								if (Match1 && Match2 && Match3 && Match4 && Match5) {
									b.setFriend(a);
									a.setFriend(b);
									numEdges+=1;
									break;
								}
							} // end of else loop
							
							cycle += 1;
							
						} // end of while loop
					} // end of demographic matching
			} // end of attempt to reach target number of friends (while loop)
			
			// remove this agent ("node") if she/he has enough friends
			if (a.getTotalFriends() == a.finalfriendsize) { iter.remove(); }
			
		} // end of iterator
		
	} // end of SocialNetworkPopulate()
	
	///////////////// SETTERS AND GETTERS
	public int getEdges() { return numEdges; }
	
} // end of AlcoholNetwork class
