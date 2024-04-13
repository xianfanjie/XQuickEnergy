package pansong291.xposed.quickenergy;

import org.json.JSONArray;
import org.json.JSONObject;
import pansong291.xposed.quickenergy.hook.AntCooperateRpcCall;
import pansong291.xposed.quickenergy.util.Config;
import pansong291.xposed.quickenergy.util.CooperationIdMap;
import pansong291.xposed.quickenergy.util.FriendIdMap;
import pansong291.xposed.quickenergy.util.Log;
import pansong291.xposed.quickenergy.util.RandomUtils;
import pansong291.xposed.quickenergy.util.Statistics;

public class AntCooperate {
    private static final String TAG = AntCooperate.class.getCanonicalName();

    public static void start() {
        if (!Config.cooperateWater())
            return;
        new Thread() {

            @Override
            public void run() {
                try {
                    while (FriendIdMap.getCurrentUid() == null || FriendIdMap.getCurrentUid().isEmpty())
                        Thread.sleep(100);
                    String s = AntCooperateRpcCall.queryUserCooperatePlantList();
                    if (s == null) {
                        Thread.sleep(RandomUtils.delay());
                        s = AntCooperateRpcCall.queryUserCooperatePlantList();
                    }
                    JSONObject jo = new JSONObject(s);
                    if ("SUCCESS".equals(jo.getString("resultCode"))) {
                        int userCurrentEnergy = jo.getInt("userCurrentEnergy");
                        JSONArray ja = jo.getJSONArray("cooperatePlants");
                        for (int i = 0; i < ja.length(); i++) {
                            jo = ja.getJSONObject(i);
                            String cooperationId = jo.getString("cooperationId");
                            if (!jo.has("name")) {
                                s = AntCooperateRpcCall.queryCooperatePlant(cooperationId);
                                jo = new JSONObject(s).getJSONObject("cooperatePlant");
                            }
                            String name = jo.getString("name");
                            int waterDayLimit = jo.getInt("waterDayLimit");
                            CooperationIdMap.putIdMap(cooperationId, name);
                            if (!Statistics.canCooperateWaterToday(FriendIdMap.getCurrentUid(), cooperationId))
                                continue;
                            int index = -1;
                            for (int j = 0; j < Config.getCooperateWaterList().size(); j++) {
                                if (Config.getCooperateWaterList().get(j).equals(cooperationId)) {
                                    index = j;
                                    break;
                                }
                            }
                            if (index >= 0) {
                                // 获取每天需要浇水的量
                                int waterCount = Config.getcooperateWaterNumList().get(index);
                                // 获取总共需要浇水的量
                                int totalWaterLimit = Config.getcooperateWaterTotalList().get(index);
                                // 获取当前用户已经浇水的量
                                int alreadyWateredCount = Statistics.getAlreadyWateredCount(FriendIdMap.getCurrentUid(), cooperationId);
                                // 计算剩余需要浇水的量
                                int remainingWaterLimit = totalWaterLimit - alreadyWateredCount;
                                // 限制每次浇水量不超过每日浇水限制、剩余需要浇水的量和当前用户的能量值中的最小值
                                waterCount = Math.min(Math.min(Math.min(waterCount, waterDayLimit), remainingWaterLimit), userCurrentEnergy);
                                // 如果每次浇水量大于0，则执行浇水操作
                                if (waterCount > 0)
                                    cooperateWater(FriendIdMap.getCurrentUid(), cooperationId, waterCount, name);
                            }
                        }
                    } else {
                        Log.i(TAG, jo.getString("resultDesc"));
                    }
                } catch (Throwable t) {
                    Log.i(TAG, "start.run err:");
                    Log.printStackTrace(TAG, t);
                }
                CooperationIdMap.saveIdMap();
            }
        }.start();
    }

    private static void cooperateWater(String uid, String coopId, int count, String name) {
        try {
            String s = AntCooperateRpcCall.cooperateWater(uid, coopId, count);
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                Log.forest("合种浇水🚿[" + name + "]" + jo.getString("barrageText"));
                Statistics.cooperateWaterToday(FriendIdMap.getCurrentUid(), coopId);
                Statistics.updateAlreadyWateredCount(uid, coopId, count);
                if (Statistics.isWateringCompleted(uid, coopId)) {
                    Config.removeCooperateWater(uid, coopId);
                    Statistics.removeAlreadyWateredCount(uid, coopId);
                }
            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.i(TAG, "cooperateWater err:");
            Log.printStackTrace(TAG, t);
        }
    }

}
