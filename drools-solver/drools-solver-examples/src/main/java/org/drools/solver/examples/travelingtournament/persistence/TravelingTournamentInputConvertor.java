package org.drools.solver.examples.travelingtournament.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.drools.solver.examples.common.persistence.XstreamSolutionDaoImpl;
import org.drools.solver.examples.travelingtournament.domain.Day;
import org.drools.solver.examples.travelingtournament.domain.Match;
import org.drools.solver.examples.travelingtournament.domain.Team;
import org.drools.solver.examples.travelingtournament.domain.TravelingTournament;

/**
 * @author Geoffrey De Smet
 */
public abstract class TravelingTournamentInputConvertor {

    private static final File inputDir = new File("data/travelingtournament/input/");

    public void convert() {
        XstreamSolutionDaoImpl solutionDao = new XstreamSolutionDaoImpl();
        File[] inputFiles = inputDir.listFiles();
        for (File inputFile : inputFiles) {
            String inputFileName = inputFile.getName();
            if (inputFileName.endsWith(".txt")) {
                TravelingTournament travelingTournament = createTravelingTournament(inputFile);
                String outputFileName = inputFileName.substring(0, inputFileName.length() - 4) + ".xml";
                File outputFile = new File(getOutputDir(), outputFileName);
                solutionDao.writeSolution(travelingTournament, outputFile);
            }
        }
    }

    private TravelingTournament createTravelingTournament(File inputFile) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(inputFile));
            return readTravelingTournament(bufferedReader);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }

    private TravelingTournament readTravelingTournament(BufferedReader bufferedReader) throws IOException {
        TravelingTournament travelingTournament = new TravelingTournament();
        travelingTournament.setId(0L);
        int n = readN(bufferedReader);
        List<Team> teamList = readTeamList(n, bufferedReader);
        travelingTournament.setTeamList(teamList);
        List<Day> dayList = constructDayList(n);
        travelingTournament.setDayList(dayList);
        List<List<Integer>> outerDistanceList = readOuterDistanceList(bufferedReader);
        List<Match> matchList = constructMatchListAndSetDistancesInTeamList(teamList, outerDistanceList);
        travelingTournament.setMatchList(matchList);
        initializeMatchDays(travelingTournament);
        return travelingTournament;
    }

    private int readN(BufferedReader bufferedReader) throws IOException {
        return Integer.parseInt( bufferedReader.readLine());
    }

    private List<Team> readTeamList(int n, BufferedReader bufferedReader) throws IOException {
        List<Team> teamList = new ArrayList<Team>();
        for (int i = 0; i < n; i++) {
            Team team = new Team();
            team.setId((long) i);
            team.setName(bufferedReader.readLine());
            team.setDistanceToTeamMap(new HashMap<Team, Integer>());
            teamList.add(team);
        }
        return teamList;
    }

    private List<List<Integer>> readOuterDistanceList(BufferedReader bufferedReader) throws IOException {
        List<List<Integer>> outerDistanceList = new ArrayList<List<Integer>>();
        String line = bufferedReader.readLine();
        while (line != null && !line.replaceAll("\\s+", "").equals("")) {
            StringTokenizer tokenizer = new StringTokenizer(line.replaceAll("\\s+", " ").trim());
            List<Integer> innerDistanceList = new ArrayList<Integer>();
            while (tokenizer.hasMoreTokens()) {
                int distance = Integer.parseInt(tokenizer.nextToken());
                innerDistanceList.add(distance);
            }
            outerDistanceList.add(innerDistanceList);
            line = bufferedReader.readLine();
        }
        return outerDistanceList;
    }

    private List<Day> constructDayList(int n) {
        List<Day> dayList = new ArrayList<Day>();
        int daySize = (n - 1) * 2; // Play vs each team (except itself) twice (home and away)
        for (int i = 0; i < daySize; i++) {
            Day day = new Day();
            day.setId((long) i);
            day.setIndex(i);
            dayList.add(day);
        }
        return dayList;
    }

    private List<Match> constructMatchListAndSetDistancesInTeamList(List<Team> teamList, List<List<Integer>> outerDistanceList) {
        List<Match> matchList = new ArrayList<Match>();
        int i = 0;
        long matchId = 0;
        for (Team homeTeam : teamList) {
            int j = 0;
            for (Team awayTeam : teamList) {
                int distance = outerDistanceList.get(i).get(j);
                homeTeam.getDistanceToTeamMap().put(awayTeam, distance);
                if (i != j) {
                    Match match = new Match();
                    match.setId(matchId);
                    matchId++;
                    match.setHomeTeam(homeTeam);
                    match.setAwayTeam(awayTeam);
                    matchList.add(match);
                }
                j++;
            }
            i++;
        }
        return matchList;
    }

    protected abstract File getOutputDir();

    protected abstract void initializeMatchDays(TravelingTournament travelingTournament);

}
