package br.com.trilha.foco;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/** Modelo local de desafios. Não cria um segundo histórico ou cronômetro. */
final class ChallengeData {
    static final int[] PERIODS = {7,14,30,60,90};
    static final int[] INTERVALS = {1,3,7,14,30};
    private ChallengeData() {}

    static void put(JSONObject o, String key, Object value) {
        try { o.put(key,value); } catch(Exception ignored) {}
    }
    static JSONArray array(JSONObject o,String key) {
        JSONArray a=o.optJSONArray(key);
        if(a==null) {a=new JSONArray();put(o,key,a);}
        return a;
    }
    static void migrate(JSONObject root) {
        array(root,"challenges");
        array(root,"learningEntries");
        array(root,"recallItems");
        array(root,"recallAttempts");
        if(root.optInt("dataVersion",0)<3) put(root,"dataVersion",3);
    }
    static JSONObject find(JSONArray arr,String id) {
        if(arr==null || id==null)return null;
        for(int i=0;i<arr.length();i++){
            JSONObject x=arr.optJSONObject(i);
            if(x!=null && id.equals(x.optString("id")))return x;
        }
        return null;
    }
    static JSONObject make() {
        JSONObject x=new JSONObject();put(x,"id",UUID.randomUUID().toString());return x;
    }
    static SimpleDateFormat dateFormat() {
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);
        f.setLenient(false);return f;
    }
    static String date(long timestamp) {return dateFormat().format(new Date(timestamp));}
    static String today() {return date(System.currentTimeMillis());}
    static String shift(String iso,int days) {
        try {
            Date v=dateFormat().parse(iso);
            Calendar c=Calendar.getInstance();c.setTime(v);c.add(Calendar.DAY_OF_YEAR,days);
            return dateFormat().format(c.getTime());
        } catch(Exception ex) {return iso;}
    }
    static String dateLabel(String iso) {
        try {
            Date value=dateFormat().parse(iso);
            return new SimpleDateFormat("dd/MM/yyyy",new Locale("pt","BR")).format(value);
        } catch(Exception e) {return iso;}
    }
    static int indexOfDate(JSONObject challenge,String iso) {
        String day=challenge.optString("startDate","");
        for(int i=0;i<challenge.optInt("days",0);i++){
            if(day.equals(iso)) return i;
            day=shift(day,1);
        }
        return -1;
    }
    static int goal(JSONObject c,String date) {
        JSONObject overrides=c.optJSONObject("dateGoals");
        if(overrides!=null && overrides.has(date))return Math.max(0,overrides.optInt(date,0));
        return Math.max(0,c.optInt("goalMinutes",40));
    }
    static JSONObject newChallenge(String name,String mode, JSONObject activity,JSONObject type,int days,int minutes) {
        JSONObject c=make();
        put(c,"name",name);put(c,"mode",mode);put(c,"activityId",activity.optString("id"));
        put(c,"typeId",type.optString("id"));put(c,"activityName",activity.optString("name"));
        put(c,"typeName",type.optString("name"));put(c,"startDate",today());
        put(c,"days",days);put(c,"goalMinutes",minutes);
        put(c,"dateGoals",new JSONObject());put(c,"createdAt",System.currentTimeMillis());
        return c;
    }
    static long focus(JSONObject session) { return Math.max(0L,session.optLong("focusMillis",session.optLong("millis",0))); }
    static boolean linked(JSONObject s,String challengeId) {return challengeId.equals(s.optString("challengeId",""));}
    static long focusOn(JSONArray sessions,String challengeId,String iso) {
        long total=0L;
        if(sessions==null)return total;
        for(int i=0;i<sessions.length();i++) {
            JSONObject s=sessions.optJSONObject(i);
            if(s!=null && linked(s,challengeId) && iso.equals(date(s.optLong("finishedAt",0L)))
                    && "completed".equals(s.optString("status","completed")))total+=focus(s);
        }
        return total;
    }
    static long metric(JSONArray sessions,String id,String property) {
        long sum=0;
        if(sessions==null)return 0;
        for(int i=0;i<sessions.length();i++){
            JSONObject s=sessions.optJSONObject(i);
            if(s!=null && linked(s,id)) {
                if("focus".equals(property))sum+=focus(s);
                else sum+=Math.max(0L,s.optLong(property,0L));
            }
        }
        return sum;
    }
    static JSONObject stats(JSONObject c,JSONArray sessions){
        JSONObject out=new JSONObject();
        String today=today(),id=c.optString("id"),start=c.optString("startDate");
        int completed=0,eligible=0,scheduled=0;long required=0,actual=0;
        int streak=0,bestStreak=0,run=0;
        for(int i=0;i<c.optInt("days",0);i++){
            String iso=shift(start,i);
            if(iso.compareTo(today)>0)break;
            int target=goal(c,iso);
            if(target<=0)continue; // Meta zero representa descanso
            eligible++;scheduled++;
            long millis=focusOn(sessions,id,iso);
            actual+=millis;required+=target*60000L;
            if(millis>=target*60000L){completed++;run++;bestStreak=Math.max(bestStreak,run);}
            else run=0;
        }
        // A sequência não é perdida por um dia ainda em andamento.
        int until=indexOfDate(c,today());
        if(until<0 && today.compareTo(start)>0) until=c.optInt("days",0)-1;
        for(int i=until;i>=0;i--) {
            String iso=shift(start,i);
            if(iso.compareTo(today)>0)continue;
            int target=goal(c,iso);
            if(target==0)continue;
            boolean done=focusOn(sessions,id,iso)>=target*60000L;
            if(!done && iso.equals(today))continue;
            if(!done)break;
            streak++;
        }
        put(out,"completed",completed);put(out,"eligible",eligible);
        put(out,"streak",streak);put(out,"bestStreak",bestStreak);
        put(out,"requiredMillis",required);put(out,"actualMillis",actual);
        put(out,"percentage",eligible==0?0:Math.round(100.0*completed/eligible));
        return out;
    }
    static JSONObject newEntry(String challengeId,String sessionId,String topic,String mainIdeas,String explanation,String doubts,int selfRating){
        JSONObject x=make();put(x,"challengeId",challengeId);put(x,"sessionId",sessionId);
        put(x,"topic",topic);put(x,"mainIdeas",mainIdeas);put(x,"explanation",explanation);
        put(x,"doubts",doubts);put(x,"selfRating",selfRating);put(x,"createdAt",System.currentTimeMillis());
        return x;
    }
    static JSONObject newRecall(String challengeId,String question,String expected) {
        JSONObject r=make();put(r,"challengeId",challengeId);put(r,"question",question);
        put(r,"expected",expected);put(r,"createdAt",System.currentTimeMillis());
        put(r,"step",0);put(r,"dueDate",shift(today(),1));return r;
    }
    static void recordRecall(JSONObject root,JSONObject item,String answer,int score) {
        JSONObject attempt=make();put(attempt,"recallId",item.optString("id"));
        put(attempt,"challengeId",item.optString("challengeId"));
        put(attempt,"answer",answer);put(attempt,"score",score);
        put(attempt,"createdAt",System.currentTimeMillis());
        array(root,"recallAttempts").put(attempt);
        int previous=item.optInt("step",0);
        int next=score==0?0:Math.min(INTERVALS.length-1,previous+1);
        if(score==1)next=Math.min(next,1); // resposta parcial repete em intervalo curto
        put(item,"step",next);put(item,"lastReviewedAt",System.currentTimeMillis());
        put(item,"dueDate",shift(today(),INTERVALS[next]));
    }
    static int dueCount(JSONArray recalls,String id) {
        int n=0;String today=today();
        for(int i=0;i<recalls.length();i++){
            JSONObject r=recalls.optJSONObject(i);
            if(r!=null && (id==null || id.equals(r.optString("challengeId"))) &&
                    r.optString("dueDate","9999-12-31").compareTo(today)<=0)n++;
        }
        return n;
    }
    static int[] recallStats(JSONArray attempts,String id) {
        int[] v={0,0};
        for(int i=0;i<attempts.length();i++){
            JSONObject a=attempts.optJSONObject(i);
            if(a!=null && id.equals(a.optString("challengeId"))){
                v[0]++;if(a.optInt("score",0)==2)v[1]++;
            }
        }
        return v;
    }
}
