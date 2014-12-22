/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package com.yimei.routing;

import java.util.Map;

import com.yimei.graph.Vertex;

// the state used in the optimization algorithms (e.g., Dijkstra's and A* algorithms)

public class State
{
	Vertex vertex; // the vertex that the state represents (a stop in the time-dependent model and an event in the time-expanded model)
	Map<String, Double> bestWeightsFromSource; // the best weights from the source vertex (e.g., travel time and number of transfers)
	Map<String, Double> lbWeightToTarget; // the lower bound of the weights to the target vertex
	
}