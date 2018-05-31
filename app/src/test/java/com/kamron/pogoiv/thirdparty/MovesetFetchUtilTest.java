package com.kamron.pogoiv.thirdparty;

import com.kamron.pogoiv.scanlogic.MovesetData;
import com.kamron.pogoiv.thirdparty.pokebattler.PokemonId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;



public class MovesetFetchUtilTest {
//    private static final String BASE_URL = "https://fight.pokebattler.com" ;
//    private static final String BASE_URL = "http://localhost:8001" ;
    private static final String BASE_URL = "https://20180530t224949-dot-fight-dot-pokebattler-1380.appspot.com" ;

    OkHttpClient httpClient;

    @Test
    public void thisShouldBeMain() throws Exception{
//        Timber.plant(new Timber.DebugTree()); This throws exceptions in unit tests
        MovesetFetchUtilTest fetcher = new MovesetFetchUtilTest();
        Map<String, List<MovesetData>> pokemon = fetcher.fetchAllPokemon();
        JSONObject toDump = new JSONObject(pokemon);
        try (FileWriter writer = new FileWriter(new File
                ("app/src/main/assets/thirdparty/pokebattler//pokemonMovesetData.json")
        )) {
            writer.write(toDump.toString());
        }

    }

    public MovesetFetchUtilTest() {
        httpClient = new OkHttpClient();
    }
    public String getAttackURL(String pokemon) {
        return BASE_URL + "/rankings/attackers/levels/30/defenders/levels/30/strategies/CINEMATIC_ATTACK_WHEN_POSSIBLE/DEFENSE_RANDOM_MC"
                + "?sort=OVERALL&dodgeStrategy=DODGE_REACTION_TIME&weatherCondition=NO_WEATHER&filterType=TOP_DEFENDER&filterValue="
                + pokemon;
    }
    public String getDefenseURL(String pokemon) {
        return BASE_URL + "/rankings/defenders/levels/30/attackers/levels/30/strategies/DEFENSE_RANDOM_MC/CINEMATIC_ATTACK_WHEN_POSSIBLE"
                + "?sort=OVERALL&dodgeStrategy=DODGE_REACTION_TIME&weatherCondition=NO_WEATHER&filterType=POKEMON&filterValue="
                + pokemon;
    }
    public Map<String, List<MovesetData>> fetchAllPokemon() {
        Map<String, List<MovesetData>> allPokemon = new TreeMap<>();
        for (PokemonId pokemon: PokemonId.values()) {
            if (pokemon == PokemonId.MISSINGNO || pokemon == PokemonId.UNRECOGNIZED) continue;

            try {
                allPokemon.put(pokemon.name(), fetchPokemon(pokemon.name()));
                Timber.i("Finished fetching %s", pokemon.name());
                //FIXME: The above doesnt properly log in unit tests
                System.out.println("Finished fetching " + pokemon.name());
            } catch (Exception e) {
                Timber.e("Unexpected error with %s", pokemon.name());
                System.err.println("Unexpected error: " + e);
                e.printStackTrace(System.err);
            }
        }
        return allPokemon;
    }


    public List<MovesetData> fetchPokemon(String pokemon) {

        TreeMap<MovesetData.Key,Double> attackScores =fetchScoreMap(getAttackURL(pokemon));
        if (attackScores == null) {
            System.err.println("Unexpected null attack scores for " + pokemon);
            return Collections.emptyList();
        }
        TreeMap<MovesetData.Key,Double> defenseScores =fetchScoreMap(getDefenseURL(pokemon));
        if (defenseScores == null) {
            System.err.println("Unexpected null defense scores for " + pokemon);
            return Collections.emptyList();
        }
        List<MovesetData> retval = new ArrayList<>(attackScores.size());
        // add all the good attack scores first
        for (Map.Entry<MovesetData.Key, Double> attackScoreEntry: attackScores.entrySet()) {
            MovesetData.Key key = attackScoreEntry.getKey();
            Double defenseScore = defenseScores.get(key);
            //TODO merge with https://fight.pokebattler.com/pokemon and https://fight.pokebattler.com/moves
            retval.add(new MovesetData(key.getQuick(), key.getCharge(), false, false, attackScoreEntry.getValue(),
            defenseScore, "UNKNOWN", "UNKNOWN"));
        }
        // then add moves that are only good on defense
        for (Map.Entry<MovesetData.Key, Double> defenseScoreEntry: defenseScores.entrySet()) {
            MovesetData.Key key = defenseScoreEntry.getKey();
            if (attackScores.containsKey(key))
                continue;
            retval.add(new MovesetData(key.getQuick(), key.getCharge(), false, false, null,
                    defenseScoreEntry.getValue(), "UNKNOWN", "UNKNOWN"));
        }
        return retval;
    }

    private TreeMap<MovesetData.Key, Double> fetchScoreMap(String url) {
        TreeMap<MovesetData.Key, Double> scores;
        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.newCall(request).execute()) {
            scores = getScoreMap(response);
        } catch(Exception e) {
            Timber.e("Could not fetch file");
            Timber.e(e);
            return new TreeMap<>();
        }
        return scores;
    }

    private TreeMap<MovesetData.Key,Double> getScoreMap( Response response) throws IOException {
        TreeMap<MovesetData.Key, Double> scores = new TreeMap<>();
        try {
            JSONObject pokemonInfo = new JSONObject(response.body().string());
            JSONArray moveRankings = pokemonInfo.getJSONArray("attackers").getJSONObject(0).getJSONArray("byMove");
            double maxScore = moveRankings.getJSONObject(0).getJSONObject("total").getDouble("overallRating");
            for (int i=0; i<moveRankings.length(); i++) {
                JSONObject move = moveRankings.getJSONObject(i);
                double score = move.getJSONObject("total").getDouble("overallRating") / maxScore;
                MovesetData.Key key = new MovesetData.Key(move.getString("move1"), move.getString("move2"));
                scores.put(key,score);
            }
        } catch (JSONException je) {
            Timber.e("Exception thrown while checking for update");
            Timber.e(je);
        }
        return scores;
    }

}