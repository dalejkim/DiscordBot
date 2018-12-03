package cache;

import java.sql.SQLException;
import java.util.Map;

import db.MySQLService;
import db.QueryService;

/**
 * 
 * I was thinking of a secondary index to speed this up either within redis or
 * locally so like an LRU or zscore with TTL or expiration to push, keep track
 * of relevant data and cache. for example if a user goes offline push the data
 * or if a user was offline for a long time and been months since the bot saw
 * them, use mysqldb
 *
 */
public class RedisService implements CacheService {

	private RedisConfig rc = new RedisConfig();
	private QueryService db = new MySQLService();
	private String userDetails, lastGame;

	// Create private variables for each with the fields for lookup instead of just
	// putting it in
	// so like private String of what we have above from userDetails and lastGame

	/**
	 * 
	 * Pattern / conditions should predict there are more unsafe setUserStats if
	 * there is more volatile updates like online/offline or other things like if we
	 * register spotify for example without using getGameType() or other checks as
	 * well as checking other metrics
	 * 
	 * @throws SQLException
	 * 
	 */
	@Override
	public void setUserStats(String key, Map<String, String> map, boolean pushUpdate) throws SQLException {
		if (pushUpdate == false) {
			rc.commands().hmset(key, map);
		} else {
			rc.commands().hmset(key, map);
			db.updateDB(key.replace(":", ",") + "," + getUserLastGameStats(key));
		}
	}

	@Override
	public String getUser(String uuid, String guild, String user) {
		userDetails = "UserUUID:" + uuid + " => " + "Guild:" + guild + " => " + "UserName:" + user;
		return userDetails;
	}

	@Override
	public String getLastPlayed(String key) {
		lastGame = rc.commands().hget(key, "last_game_played");
		return lastGame != null ? lastGame : "none!";
	}

	@Override
	public String getLastGamePlayTime(String key) {
		return rc.commands().hget(key, "new_game_start_time");
	}

	/**
	 * Get specific user stats from this (logic could be much better like secondary
	 * cache to fix and index so that I don't have to do a second instruction on
	 * checking the db or not
	 * 
	 * @throws SQLException
	 * 
	 * 
	 *                      Also for here it used to be a public List<Object>
	 *                      Because when we work with Lettuce thing are of
	 *                      List<Key<String,String>> value when we are working with
	 *                      redis And that's how it communicates the items / objects
	 *                      in redis.
	 * 
	 *                      However for performance and high speed we return String
	 *                      instead of List<Object> or List<Key<String,String>> It's
	 *                      faster but it goes back to the security issue however
	 *                      since the data here isn't sensitive or private We have
	 *                      no worries but if it were sensitive data rethink how
	 *                      objects travel between
	 * 
	 *                      final String joined = list.stream() .map(item -> "(" +
	 *                      item + ")") .collect(joining(" "));
	 */

	public String getUserLastGameStats(String key) throws SQLException {
		return rc.commands().hmget(key, "username", "last_game_played", "when_last_game_played", "last_game_play_time")
				.toString();

//		return lastGameExists(key) == true
//				? rc.commands().hmget(key, userName, "last_game_played", whenLastGamePlayed, lastGamePlayTime)
//						.toString()
//				: db.pullUserLastGameStats(key) != null ? db.pullUserLastGameStats(key).toString()
//						/* && pushIntoCache */ : "No last game stats!";
//		
//		if(lastGameExists(key)) {
//			List<Object> list = Arrays.asList(rc.commands().hmget(key, userName, lastGamePlayed, whenLastGamePlayed, lastGamePlayTime));
//			return Arrays.asList(rc.commands().hmget(key, userName, lastGamePlayed, whenLastGamePlayed, lastGamePlayTime));
//		} else {
//			return db.getUserLastGameStats(userUUID);
//		}
//		System.out.println(rc.commands().hmget(key, userName, lastGamePlayed, whenLastGamePlayed, lastGamePlayTime));
//		return rc.commands().hmget(key, userName, lastGamePlayed, whenLastGamePlayed, lastGamePlayTime);

	}

	@Override
	public boolean lastGameExists(String key) {
		return rc.commands().hexists(key, "last_game_played");
	}

	@Override
	public void removeLastGameStats(String key) {
		rc.commands().hdel(key, "last_game_played");
	}

	@Override
	public Map<String, String> getUserStats(String key) {
		return rc.commands().hgetall(key);
	}

	@Override
	public boolean exists(String key) {
		// 1 means it exists in the redis cache
		if (rc.commands().exists(key) == 1) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String removeUserFromServer(String key) {
		rc.commands().del(key);
		return key;
	}
}