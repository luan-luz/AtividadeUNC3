package br.com.trilha.foco;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputFilter;
import android.text.InputType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import android.content.Intent;
import android.net.Uri;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

/** 1o v1.2: aplicativo Android nativo offline; preparação, foco planejado e excedente. */
public final class MainActivity extends Activity {
    private static final int INK = Color.WHITE;
    private static final int MUTED = Color.rgb(166, 166, 173);
    private static final int ACCENT = Color.rgb(224, 35, 65);
    private static final int BG = Color.rgb(11, 11, 14);
    private static final int SURFACE = Color.rgb(26, 26, 32);
    private static final int WHITE = Color.WHITE;
    private static final int LINE = Color.rgb(55, 55, 64);
    private static final int SOFT = Color.rgb(36, 36, 43);
    private static final int DANGER = Color.rgb(255, 104, 116);

    private JSONObject data;
    private String page = "home";
    private String selectedActivity = "";
    private String selectedChallenge = "";
    private String pendingLearningSession = "";
    private static final int BACKUP_EXPORT = 71, BACKUP_IMPORT = 72;
    private TextView clockView;
    private TextView clockStatus;
    private TextView clockSecondary;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if ("timer".equals(page) && clockView != null) {
                JSONObject timer = data.optJSONObject("timer");
                if (timer != null) {
                    clockView.setText(clockFormat(elapsed()));
                    if (clockStatus != null) clockStatus.setText(timerStatus(timer));
                    if (clockSecondary != null) clockSecondary.setText(timerDetails(timer));
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        load();
        if (saved != null) {
            page = saved.getString("page", "home");
            selectedActivity = saved.getString("activity", "");
            selectedChallenge = saved.getString("challenge", "");
            pendingLearningSession = saved.getString("learningSession", "");
        }
        render();
        handler.post(tick);
        UpdateManager.check(this,false);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("page", page);
        out.putString("activity", selectedActivity);
        out.putString("challenge", selectedChallenge);
        out.putString("learningSession", pendingLearningSession);
        super.onSaveInstanceState(out);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if ("timer".equals(page) || "detail".equals(page)) navigate("activities");
        else if ("challengeDetail".equals(page) || "reviews".equals(page) || "learningCapture".equals(page)) navigate("challenges");
        else if (!"home".equals(page)) navigate("home");
        else super.onBackPressed();
    }

    private void load() {
        SharedPreferences p = getSharedPreferences("trilha_data", MODE_PRIVATE);
        String raw = p.getString("database", "");
        try { data = new JSONObject(raw); } catch (Exception e) { data = new JSONObject(); }
        if (data.optJSONArray("activities") == null) {
            JSONArray a = new JSONArray();
            a.put(activity("Leitura", "Leitura de estudo", "Leitura de livro", "Revisão"));
            a.put(activity("Exercícios", "Caminhada", "Corrida", "Musculação"));
            a.put(activity("Organização", "Planejamento", "Tarefas de casa"));
            set(data, "activities", a);
        }
        if (data.optJSONArray("sessions") == null) set(data, "sessions", new JSONArray());
        ChallengeData.migrate(data);
        JSONObject oldTimer = data.optJSONObject("timer");
        if (oldTimer != null && !oldTimer.has("phase")) {
            // Migração: sessão anterior já era foco sem tempo planejado.
            set(oldTimer, "phase", "focus");
            set(oldTimer, "prepMillis", 0L);
            set(oldTimer, "plannedMillis", 0L);
            set(oldTimer, "createdAt", oldTimer.optLong("startedAt", System.currentTimeMillis()));
        }
        save();
    }
    private void save() { getSharedPreferences("trilha_data", MODE_PRIVATE).edit().putString("database", data.toString()).apply(); }
    private static void set(JSONObject obj, String k, Object v) { try { obj.put(k, v); } catch (Exception ignored) { } }
    private static JSONObject record(String name) {
        JSONObject o = new JSONObject(); set(o, "id", UUID.randomUUID().toString()); set(o, "name", name); return o;
    }
    private static JSONObject activity(String name, String... types) {
        JSONObject a = record(name); JSONArray ta = new JSONArray();
        for (String t : types) ta.put(record(t)); set(a, "types", ta); return a;
    }
    private JSONArray activities() { return data.optJSONArray("activities"); }
    private JSONArray sessions() { return data.optJSONArray("sessions"); }
    private JSONArray challenges() { return ChallengeData.array(data, "challenges"); }
    private JSONArray entries() { return ChallengeData.array(data, "learningEntries"); }
    private JSONArray recalls() { return ChallengeData.array(data, "recallItems"); }
    private JSONArray attempts() { return ChallengeData.array(data, "recallAttempts"); }
    private JSONObject currentChallenge() {return ChallengeData.find(challenges(),selectedChallenge);}
    private JSONObject find(JSONArray arr, String id) {
        if (arr == null) return null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return o;
        }
        return null;
    }
    private JSONObject chosen() { return find(activities(), selectedActivity); }
    private JSONArray remove(JSONArray arr, String id) {
        JSONArray next = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && !id.equals(o.optString("id"))) next.put(o);
        }
        return next;
    }
    private boolean duplicate(JSONArray arr, String name, String ignoreId) {
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && !ignoreId.equals(o.optString("id")) && name.equalsIgnoreCase(o.optString("name"))) return true;
        }
        return false;
    }
    private String clean(String s) { return s.trim().replaceAll("\\s+", " "); }
    private int dp(float px) { return Math.round(px * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int fill, int round, int border) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill); g.setCornerRadius(dp(round));
        if (border != 0) g.setStroke(dp(1), border);
        return g;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextColor(color); v.setTextSize(size);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setIncludeFontPadding(true);
        return v;
    }
    private void space(LinearLayout target, int px) {
        View spacer = new View(this); target.addView(spacer, params(1, px));
    }
    private LinearLayout card() {
        LinearLayout c = column(); c.setPadding(dp(17), dp(17), dp(17), dp(17));
        c.setBackground(shape(SURFACE, 20, LINE)); return c;
    }
    private void action(LinearLayout container, String label, boolean primary, Runnable click) {
        TextView b = text(label, 14, primary ? WHITE : ACCENT, true);
        b.setGravity(Gravity.CENTER); b.setMinHeight(dp(49));
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        b.setBackground(shape(primary ? ACCENT : SOFT, 14, primary ? 0 : LINE));
        b.setOnClickListener(v -> click.run());
        b.setFocusable(true); b.setClickable(true);
        container.addView(b, params(-1, -2));
    }
    private TextView smallAction(String label, Runnable click, int color) {
        TextView t = text(label, 12, color, true);
        t.setPadding(dp(11), dp(9), dp(11), dp(9)); t.setGravity(Gravity.CENTER);
        t.setBackground(shape(color == DANGER ? 0xFF442028 : SOFT, 10, 0));
        t.setOnClickListener(v -> click.run());
        t.setFocusable(true); t.setClickable(true);
        return t;
    }
    private void section(LinearLayout body, String headline, String description) {
        TextView h = text(headline, 26, INK, true);
        body.addView(h);
        if (!description.isEmpty()) { space(body, 7); body.addView(text(description, 13, MUTED, false)); }
        space(body, 21);
    }
    private void overline(LinearLayout body, String value) {
        TextView t = text(value.toUpperCase(new Locale("pt", "BR")), 11, ACCENT, true);
        t.setLetterSpacing(0.13f); body.addView(t); space(body, 9);
    }
    private void separator(LinearLayout target) {
        View line = new View(this); line.setBackgroundColor(LINE);
        LinearLayout.LayoutParams p = params(-1, 1); p.topMargin=dp(11);p.bottomMargin=dp(11);
        target.addView(line, p);
    }
    private void navigate(String dest) { page = dest; render(); }
    private void render() {
        clockView = null; clockStatus = null; clockSecondary = null;
        LinearLayout shell = column(); shell.setBackgroundColor(BG);
        LinearLayout top = row(); top.setPadding(dp(22), dp(12), dp(22), dp(12));
        TextView brand = text("1o", 27, WHITE, true);
        brand.setLetterSpacing(-0.07f);
        top.addView(brand, params(-2, 38));
        View accentBar = new View(this);accentBar.setBackgroundColor(ACCENT);
        LinearLayout.LayoutParams accentParams=params(3,23);accentParams.leftMargin=dp(12);
        top.addView(accentBar,accentParams);
        shell.addView(top, params(-1, -2));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(false); scroll.setClipToPadding(false);
        LinearLayout body = column(); body.setPadding(dp(22), dp(23), dp(22), dp(35));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams sc = new LinearLayout.LayoutParams(-1, 0, 1);
        shell.addView(scroll, sc);
        switch (page) {
            case "activities": renderActivities(body); break;
            case "detail": renderDetail(body); break;
            case "timer": renderTimer(body); break;
            case "history": renderHistory(body); break;
            case "challenges": renderChallenges(body); break;
            case "challengeDetail": renderChallengeDetail(body); break;
            case "reviews": renderReviews(body); break;
            case "learningCapture": renderLearningCapture(body); break;
            default: renderHome(body); break;
        }
        if (!"timer".equals(page)) shell.addView(nav(), params(-1, 71));
        setContentView(shell);
    }
    private LinearLayout nav() {
        LinearLayout nav = row();nav.setPadding(dp(8),dp(5),dp(8),dp(5));
        nav.setBackgroundColor(SURFACE);nav.setGravity(Gravity.CENTER);nav.setWeightSum(4f);
        String[] ids = {"home", "activities", "challenges", "history"};
        String[] labels = {"Início", "Atividades", "Desafios", "Histórico"};
        String[] icons = {"⌂", "☷", "◇", "◷"};
        for (int i=0; i<ids.length; i++) {
            final String dest=ids[i];
            LinearLayout item=column(); item.setGravity(Gravity.CENTER);
            TextView ic=text(icons[i], 25, page.equals(dest)||("detail".equals(page)&&"activities".equals(dest))?ACCENT:MUTED, true);
            TextView label=text(labels[i], 11, page.equals(dest)||("detail".equals(page)&&"activities".equals(dest))?ACCENT:MUTED, true);
            item.addView(ic); item.addView(label);
            item.setOnClickListener(v->navigate(dest)); item.setFocusable(true); item.setClickable(true);
            LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-1,1f);
            nav.addView(item,ip);
        }
        return nav;
    }

    private long elapsed() {
        JSONObject t = data.optJSONObject("timer"); if (t == null) return 0;
        long millis = t.optLong("savedMillis", 0);
        if (!t.optBoolean("paused", false)) millis += Math.max(0, System.currentTimeMillis() - t.optLong("startedAt", System.currentTimeMillis()));
        return Math.max(0, millis);
    }
    private boolean isPreparation(JSONObject t) { return "preparation".equals(t.optString("phase", "focus")); }
    private long preparationMillis(JSONObject t) {
        return isPreparation(t) ? elapsed() : t.optLong("prepMillis", 0L);
    }
    private long focusMillis(JSONObject t) {
        return isPreparation(t) ? 0L : elapsed();
    }
    private long extraMillis(JSONObject t) {
        long planned = t.optLong("plannedMillis", 0L);
        return planned > 0 ? Math.max(0L, focusMillis(t) - planned) : 0L;
    }
    private String timerStatus(JSONObject t) {
        String status = isPreparation(t) ? "PREPARAÇÃO" : "FOCO";
        if (t.optBoolean("paused", false)) status += " · PAUSADO";
        else if (!isPreparation(t) && extraMillis(t) > 0) status += " · TEMPO EXTRA";
        return status;
    }
    private String timerDetails(JSONObject t) {
        if (isPreparation(t)) return "Sem limite de tempo · Meta de foco: " + human(t.optLong("plannedMillis", 0));
        long planned = t.optLong("plannedMillis", 0L);
        if (planned == 0) return "Sessão antiga sem meta · Preparação: não registrada";
        long focus = focusMillis(t);
        String relation = focus >= planned
                ? "+ " + clockFormat(focus - planned) + " de tempo extra"
                : clockFormat(planned - focus) + " até a meta";
        return "Planejado: " + human(planned) + " · Preparação: " + human(preparationMillis(t)) + "\n" + relation;
    }

    private String clockFormat(long millis) {
        long s=millis/1000; return String.format(Locale.ROOT,"%02d:%02d:%02d", s/3600, (s/60)%60, s%60);
    }
    private String human(long millis) {
        long mins = millis/60000; long s=(millis/1000)%60;
        if (mins >= 60) return String.format(Locale.ROOT, "%dh %02dmin", mins/60, mins%60);
        if (mins > 0) return mins + " min";
        return s + " s";
    }
    private String day(long when) {
        return new SimpleDateFormat("dd/MM/yyyy · HH:mm", new Locale("pt", "BR")).format(new Date(when));
    }
    private boolean today(long when) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.ROOT);
        return fmt.format(new Date(when)).equals(fmt.format(new Date()));
    }

    private void renderHome(LinearLayout b) {
        overline(b, "Painel");
        section(b, "Atividades e sessões", "Tempo registrado e acesso rápido às suas atividades.");
        long sum=0; int count=0;
        for (int i=0;i<sessions().length();i++) {
            JSONObject s = sessions().optJSONObject(i);
            if (s!=null && today(s.optLong("finishedAt",0))) {sum += s.optLong("millis",0); count++;}
        }
        LinearLayout metric=column(); metric.setPadding(dp(22),dp(22),dp(22),dp(21)); metric.setBackground(shape(SURFACE,20,LINE));
        metric.addView(text("Tempo de foco hoje",13,MUTED,false)); space(metric,6);
        metric.addView(text(human(sum),39,WHITE,true)); space(metric,9);
        metric.addView(text(count+" sessão(ões) concluída(s)",12,MUTED,false));
        b.addView(metric, params(-1,-2)); space(b,15);
        LinearLayout start=card(); start.addView(text("Sessão de trabalho",18,INK,true)); space(start,8);
        start.addView(text("Defina uma meta, registre a preparação e execute o bloco de foco.",13,MUTED,false)); space(start,16);
        if (data.optJSONObject("timer") != null) {
            action(start,"▶  Continuar sessão",true,()->navigate("timer"));
        } else {
            action(start,"Escolher atividade  →",true,()->navigate("activities"));
        }
        b.addView(start,params(-1,-2)); space(b,14);
        int due=ChallengeData.dueCount(recalls(),null);
        LinearLayout quick=card();quick.addView(text("DESAFIOS",11,ACCENT,true));space(quick,8);
        quick.addView(text(challenges().length()+" desafio(s) · "+due+" revisão(ões) para hoje",14,WHITE,false));space(quick,12);
        action(quick,"Abrir desafios",false,()->navigate("challenges"));
        b.addView(quick,params(-1,-2));space(b,24);
        overline(b,"Suas atividades");
        if (activities().length()==0) b.addView(text("Nenhuma atividade cadastrada.",14,MUTED,false));
        for (int i=0;i<Math.min(activities().length(),4);i++) {
            JSONObject a=activities().optJSONObject(i); if (a!=null) activityLine(b,a);
        }
        space(b,14); action(b,"+  Nova atividade",false,()->editActivity(null));
        space(b,28);overline(b,"Aplicativo");
        LinearLayout updates=card();
        updates.addView(text("Atualizações do 1o",16,WHITE,true));space(updates,6);
        updates.addView(text("Consulte novas versões pelo GitHub. O acesso à internet é opcional e nunca bloqueia o uso do aplicativo.",12,MUTED,false));space(updates,12);
        action(updates,"Verificar atualizações",false,()->UpdateManager.check(this,true));
        b.addView(updates);
    }
    private void activityLine(LinearLayout b, JSONObject a) {
        LinearLayout line=card(); LinearLayout top=row();
        LinearLayout name=column(); name.addView(text(a.optString("name"),16,INK,true)); space(name,3);
        JSONArray ta=a.optJSONArray("types");
        name.addView(text((ta==null?0:ta.length())+" tipo(s) cadastrado(s)",12,MUTED,false));
        top.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        top.addView(smallAction("Abrir  →",()->{selectedActivity=a.optString("id");navigate("detail");},ACCENT));
        line.addView(top); b.addView(line,params(-1,-2)); space(b,10);
    }
    private void renderActivities(LinearLayout b) {
        overline(b,"Cadastro");
        section(b,"Minhas atividades","Crie atividades e organize os tipos específicos de cada uma.");
        action(b,"+  Cadastrar atividade",true,()->editActivity(null)); space(b,20);
        if (activities().length()==0) b.addView(text("Nenhuma atividade cadastrada. Toque no botão acima para começar.",13,MUTED,false));
        for (int i=0;i<activities().length();i++) {
            JSONObject a=activities().optJSONObject(i); if (a!=null) activityLine(b,a);
        }
    }
    private void renderDetail(LinearLayout b) {
        JSONObject a=chosen(); if (a==null) {navigate("activities");return;}
        overline(b,"Atividade e tipos");
        section(b,a.optString("name"),"Escolha um tipo e determine o tempo planejado.");
        LinearLayout actions=row();
        actions.addView(smallAction("Renomear",()->editActivity(a),ACCENT));
        spaceHorizontal(actions,10);
        actions.addView(smallAction("Excluir",()->confirmDeleteActivity(a),DANGER));
        b.addView(actions);space(b,25);
        overline(b,"Tipos cadastrados");
        JSONArray types=a.optJSONArray("types");
        if (types!=null) for(int i=0;i<types.length();i++) {
            JSONObject t=types.optJSONObject(i); if (t==null) continue;
            LinearLayout c=card();c.addView(text(t.optString("name"),16,INK,true)); space(c,12);
            LinearLayout r=row(); r.addView(smallAction("▶  Iniciar",()->start(a,t),ACCENT));
            spaceHorizontal(r,8); r.addView(smallAction("Editar",()->editType(t),ACCENT));
            spaceHorizontal(r,8); r.addView(smallAction("Excluir",()->confirmDeleteType(t),DANGER));
            c.addView(r);b.addView(c,params(-1,-2));space(b,10);
        }
        if(types==null||types.length()==0) {
            b.addView(text("Esta atividade ainda não possui tipos. Crie o primeiro para começar o cronômetro.",13,MUTED,false));
            space(b,18);
        }
        action(b,"+  Cadastrar novo tipo",false,()->editType(null));
        space(b,22);
        action(b,"←  Voltar às atividades",false,()->navigate("activities"));
    }
    private void spaceHorizontal(LinearLayout r,int px) { r.addView(new View(this),params(px,1)); }
    private void renderTimer(LinearLayout b) {
        JSONObject t=data.optJSONObject("timer");
        if (t==null) {navigate("activities");return;}
        boolean preparing=isPreparation(t);
        overline(b, "Sessão ativa");
        section(b,t.optString("activityName"),t.optString("typeName"));
        LinearLayout info=card();
        info.addView(text("META DE FOCO",11,ACCENT,true));space(info,6);
        info.addView(text(human(t.optLong("plannedMillis",0L)),26,WHITE,true));space(info,7);
        info.addView(text("Preparação sem prazo. O foco continua contando após a meta.",12,MUTED,false));
        b.addView(info,params(-1,-2));space(b,25);
        LinearLayout clockPanel=column();clockPanel.setGravity(Gravity.CENTER);
        clockPanel.setPadding(dp(14),dp(28),dp(14),dp(28));
        clockPanel.setBackground(shape(SURFACE,24,ACCENT));
        clockStatus=text(timerStatus(t),12,ACCENT,true);clockStatus.setLetterSpacing(0.08f);
        clockStatus.setGravity(Gravity.CENTER);clockPanel.addView(clockStatus);space(clockPanel,12);
        clockView=text(clockFormat(elapsed()),39,WHITE,true);
        clockView.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);clockView.setGravity(Gravity.CENTER);
        clockPanel.addView(clockView);space(clockPanel,13);
        clockSecondary=text(timerDetails(t),13,MUTED,false);
        clockSecondary.setGravity(Gravity.CENTER);clockPanel.addView(clockSecondary);
        b.addView(clockPanel,params(-1,-2));space(b,22);
        if(!t.optString("challengeId","").isEmpty()) {
            JSONObject ch=ChallengeData.find(challenges(),t.optString("challengeId"));
            if(ch!=null){b.addView(text("DESAFIO · "+ch.optString("name"),12,ACCENT,true));space(b,12);}
        }
        if(preparing) {
            action(b,"Iniciar bloco de foco  →",true,()->beginFocus());space(b,10);
            action(b,t.optBoolean("paused",false)?"Retomar preparação":"Pausar preparação",false,()->toggleTimer());
            space(b,10);action(b,"Encerrar sem iniciar foco",false,()->finishTimer());
        } else {
            action(b,t.optBoolean("paused",false)?"Retomar foco":"Pausar foco",true,()->toggleTimer());
            space(b,10);action(b,"Concluir e salvar sessão",false,()->finishTimer());
        }
        space(b,10);action(b,"Voltar sem encerrar",false,()->navigate("home"));
        space(b,12);
        TextView discard=text("Descartar sem registrar",13,DANGER,true);discard.setGravity(Gravity.CENTER);
        discard.setPadding(0,dp(15),0,dp(15));discard.setOnClickListener(v->confirmDiscard());
        b.addView(discard);
    }
    private void renderHistory(LinearLayout b) {
        overline(b,"Registros locais");
        section(b,"Histórico","Preparação, foco, metas e tempo extra armazenados no aparelho.");
        long totalFocus=0L,totalPrep=0L,totalExtra=0L;
        int prepRecords=0,plannedRecords=0,met=0,abandoned=0;
        for(int i=0;i<sessions().length();i++) {
            JSONObject s=sessions().optJSONObject(i);if(s==null)continue;
            totalFocus+=s.optLong("focusMillis",s.optLong("millis",0L));
            if(s.has("prepMillis")) {totalPrep+=s.optLong("prepMillis",0L);prepRecords++;}
            if("preparation_only".equals(s.optString("status"))) abandoned++;
            if("completed".equals(s.optString("status")) && s.optLong("plannedMillis",0)>0) {
                plannedRecords++;
                if(s.optLong("focusMillis",0)>=s.optLong("plannedMillis",0)) met++;
            }
            totalExtra+=s.optLong("extraMillis",0L);
        }
        LinearLayout metric=card();metric.addView(text("TEMPO DE FOCO",11,ACCENT,true));space(metric,6);
        metric.addView(text(human(totalFocus),32,WHITE,true));space(metric,7);
        metric.addView(text(sessions().length()+" registros no dispositivo",12,MUTED,false));
        b.addView(metric,params(-1,-2));space(b,11);
        LinearLayout summary=card();
        summary.addView(text("Preparação registrada: "+human(totalPrep),13,INK,false));space(summary,8);
        summary.addView(text("Tempo extra registrado: "+human(totalExtra),13,INK,false));space(summary,8);
        summary.addView(text("Preparação média: "+(prepRecords>0?human(totalPrep/prepRecords):"sem dados"),13,INK,false));space(summary,8);
        summary.addView(text("Metas alcançadas: "+met+" de "+plannedRecords,13,INK,false));space(summary,8);
        summary.addView(text("Sessões encerradas na preparação: "+abandoned,13,INK,false));space(summary,12);
        summary.addView(text("Os números descrevem comportamentos registrados, mas não determinam, isoladamente, procrastinação ou desempenho.",12,MUTED,false));
        b.addView(summary,params(-1,-2));space(b,20);
        if(sessions().length()==0) b.addView(text("Nenhuma sessão registrada até agora.",13,MUTED,false));
        for(int i=sessions().length()-1;i>=0;i--) {
            JSONObject s=sessions().optJSONObject(i);if(s==null)continue;
            LinearLayout c=card();
            c.addView(text(s.optString("activityName"),17,INK,true));space(c,4);
            c.addView(text(s.optString("typeName"),13,MUTED,false));space(c,8);
            JSONObject ch=ChallengeData.find(challenges(),s.optString("challengeId"));
            if(ch!=null){c.addView(text("Desafio: "+ch.optString("name"),12,ACCENT,true));space(c,8);} 
            space(c,4);
            if(s.has("prepMillis")) {
                c.addView(text("Preparação: "+human(s.optLong("prepMillis",0L)),13,MUTED,false));space(c,6);
                c.addView(text("Foco: "+human(s.optLong("focusMillis",0L))+" / Planejado: "+human(s.optLong("plannedMillis",0L)),13,INK,false));space(c,6);
                c.addView(text("Extra: +"+human(s.optLong("extraMillis",0L)),13,ACCENT,true));space(c,6);
                if("preparation_only".equals(s.optString("status"))) {
                    c.addView(text("Encerrada antes do foco",12,DANGER,true));space(c,6);
                }
            } else {
                c.addView(text("Foco: "+human(s.optLong("millis",0L)),13,INK,false));space(c,6);
                c.addView(text("Registro anterior · Preparação e meta não disponíveis",12,MUTED,false));space(c,6);
            }
            c.addView(text(day(s.optLong("finishedAt",0L)),12,MUTED,false));
            b.addView(c,params(-1,-2));space(b,11);
        }
    }

    // DESAFIOS: as sessões existentes são a única fonte para tempos e constância.
    private void renderChallenges(LinearLayout b) {
        overline(b,"Módulo de desafios");
        section(b,"Desafios","Metas diárias de 7 a 90 dias. Execução e aprendizagem sem conexão.");
        action(b,"+  Criar desafio",true,()->chooseChallengeType());space(b,15);
        int due=ChallengeData.dueCount(recalls(),null);
        LinearLayout rev=card();
        rev.addView(text("Revisões programadas",16,WHITE,true));space(rev,7);
        rev.addView(text(due+" revisão(ões) previstas ou atrasadas",13,MUTED,false));space(rev,12);
        action(rev,"Abrir fila de revisões",false,()->{selectedChallenge="";navigate("reviews");});
        b.addView(rev);space(b,15);
        if(challenges().length()==0) {
            b.addView(text("Nenhum desafio criado. Use uma atividade existente e estabeleça sua meta.",14,MUTED,false));
        }
        for(int i=challenges().length()-1;i>=0;i--) {
            JSONObject c=challenges().optJSONObject(i);if(c==null)continue;
            JSONObject m=ChallengeData.stats(c,sessions());
            LinearLayout tile=card();
            tile.addView(text(c.optString("name"),18,WHITE,true));space(tile,6);
            tile.addView(text(("learning".equals(c.optString("mode"))?"Aprendizagem":"Execução")+" · "+c.optInt("days")+" dias · "+c.optString("typeName"),12,MUTED,false));space(tile,10);
            tile.addView(text(m.optInt("completed")+" / "+m.optInt("eligible")+" dias cumpridos até hoje",14,WHITE,true));space(tile,7);
            tile.addView(text("Constância: "+m.optInt("percentage")+"% · sequência: "+m.optInt("streak"),12,MUTED,false));space(tile,12);
            action(tile,"Abrir desafio  →",false,()->{selectedChallenge=c.optString("id");navigate("challengeDetail");});
            b.addView(tile);space(b,12);
        }
        space(b,12);
        action(b,"Exportar cópia dos dados (JSON)",false,()->exportBackup());
        space(b,10);
        action(b,"Restaurar uma cópia de segurança",false,()->importBackup());
    }

    private EditText field(String hint,String initial,boolean multiline) {
        EditText f=new EditText(this);f.setTextColor(WHITE);f.setHintTextColor(MUTED);f.setTextSize(15);
        f.setHint(hint);f.setText(initial);
        f.setSingleLine(!multiline);
        if(multiline){f.setMinLines(2);f.setMaxLines(5);f.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        else f.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        f.setPadding(dp(12),dp(12),dp(12),dp(12));
        f.setBackground(shape(SOFT,10,LINE));
        return f;
    }
    private EditText numberField(String hint,String initial) {
        EditText f=field(hint,initial,false);f.setInputType(InputType.TYPE_CLASS_NUMBER);return f;
    }
    private LinearLayout dialogFields(EditText... fields) {
        LinearLayout box=column();box.setPadding(dp(18),dp(8),dp(18),dp(5));
        for(EditText f:fields){box.addView(f,params(-1,-2));space(box,12);}return box;
    }
    private int parseMinutes(EditText f,int min,int max) {
        try {int n=Integer.parseInt(f.getText().toString().trim());if(n>=min&&n<=max)return n;}
        catch(Exception ignored){}
        f.setError("Informe valor entre "+min+" e "+max+".");return -1;
    }
    private void chooseChallengeType() {
        new AlertDialog.Builder(this).setTitle("Tipo de desafio")
            .setItems(new String[]{"Execução · constância e tempo","Aprendizagem · compreensão e revisões"},
                (d,which)->chooseChallengeActivity(which==1?"learning":"execution"))
            .setNegativeButton("Cancelar",null).show();
    }
    private void chooseChallengeActivity(String mode) {
        ArrayList<JSONObject> acts=new ArrayList<>();
        ArrayList<JSONObject> types=new ArrayList<>();
        ArrayList<String> names=new ArrayList<>();
        for(int i=0;i<activities().length();i++) {
            JSONObject a=activities().optJSONObject(i);if(a==null)continue;
            JSONArray ts=a.optJSONArray("types");
            if(ts==null)continue;
            for(int j=0;j<ts.length();j++){
                JSONObject t=ts.optJSONObject(j);if(t==null)continue;
                acts.add(a);types.add(t);names.add(a.optString("name")+" / "+t.optString("name"));
            }
        }
        if(names.isEmpty()){notice("Cadastre uma atividade e um tipo primeiro.");navigate("activities");return;}
        new AlertDialog.Builder(this).setTitle("Vincular atividade existente")
            .setItems(names.toArray(new String[0]),(d,i)->chooseChallengeDuration(mode,acts.get(i),types.get(i)))
            .setNegativeButton("Cancelar",null).show();
    }
    private void chooseChallengeDuration(String mode,JSONObject act,JSONObject type) {
        String[] durations={"7 dias","14 dias","30 dias","60 dias","90 dias"};
        new AlertDialog.Builder(this).setTitle("Duração do desafio")
            .setItems(durations,(d,i)->challengeForm(mode,act,type,ChallengeData.PERIODS[i]))
            .setNegativeButton("Cancelar",null).show();
    }
    private void challengeForm(String mode,JSONObject act,JSONObject type,int days) {
        EditText name=field("Nome do desafio",type.optString("name")+" · "+days+" dias",false);
        EditText minutes=numberField("Meta diária em minutos","learning".equals(mode)?"40":"20");
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Novo desafio · "+days+" dias")
            .setView(dialogFields(name,minutes)).setNegativeButton("Cancelar",null)
            .setPositiveButton("Criar desafio",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String title=clean(name.getText().toString());
            if(title.length()<2||title.length()>60){name.setError("Use de 2 a 60 caracteres.");return;}
            int amount=parseMinutes(minutes,1,720);if(amount<0)return;
            JSONObject c=ChallengeData.newChallenge(title,mode,act,type,days,amount);
            challenges().put(c);save();dialog.dismiss();selectedChallenge=c.optString("id");navigate("challengeDetail");
        }));
        dialog.show();
    }
    private void renderChallengeDetail(LinearLayout b) {
        JSONObject c=currentChallenge();
        if(c==null){navigate("challenges");return;}
        JSONObject stats=ChallengeData.stats(c,sessions());
        String today=ChallengeData.today();int idx=ChallengeData.indexOfDate(c,today);
        boolean active=idx>=0;
        overline(b,"Desafio · "+("learning".equals(c.optString("mode"))?"aprendizagem":"execução"));
        section(b,c.optString("name"),c.optString("activityName")+" / "+c.optString("typeName"));
        LinearLayout panel=card();
        panel.addView(text("CONSTÂNCIA",11,ACCENT,true));space(panel,7);
        panel.addView(text(stats.optInt("completed")+" / "+stats.optInt("eligible")+" dias",28,WHITE,true));space(panel,5);
        panel.addView(text("Cumprimento: "+stats.optInt("percentage")+"% · sequência: "+stats.optInt("streak")+" · melhor: "+stats.optInt("bestStreak"),12,MUTED,false));space(panel,9);
        panel.addView(text("Foco acumulado: "+human(ChallengeData.metric(sessions(),c.optString("id"),"focus")),13,WHITE,false));space(panel,6);
        panel.addView(text("Preparação: "+human(ChallengeData.metric(sessions(),c.optString("id"),"prepMillis")),13,MUTED,false));space(panel,6);
        panel.addView(text("Excedente das sessões: "+human(ChallengeData.metric(sessions(),c.optString("id"),"extraMillis")),13,MUTED,false));
        b.addView(panel);space(b,15);
        LinearLayout daily=card();
        daily.addView(text(active?"DIA "+(idx+1)+" DE "+c.optInt("days"):"PERÍODO DO DESAFIO",12,ACCENT,true));space(daily,7);
        if(active){
            int goal=ChallengeData.goal(c,today);
            long done=ChallengeData.focusOn(sessions(),c.optString("id"),today);
            daily.addView(text(goal==0?"Dia de descanso":"Hoje: "+human(done)+" / "+goal+" min",16,WHITE,true));
            space(daily,8);
            if(goal>0){
                action(daily,"▶  Iniciar sessão do desafio",true,()->startChallenge(c));
            } else daily.addView(text("Meta definida como descanso para hoje.",13,MUTED,false));
            space(daily,10);
        } else daily.addView(text("Início: "+ChallengeData.dateLabel(c.optString("startDate"))+" · duração: "+c.optInt("days")+" dias",13,MUTED,false));
        action(daily,"Personalizar meta de um dia",false,()->chooseDayGoal(c));
        space(daily,10);action(daily,"Alterar meta diária padrão",false,()->changeDefaultGoal(c));
        b.addView(daily);space(b,14);
        if("learning".equals(c.optString("mode"))) {
            LinearLayout learning=card();
            learning.addView(text("APRENDIZAGEM",11,ACCENT,true));space(learning,9);
            int[] scores=ChallengeData.recallStats(attempts(),c.optString("id"));
            learning.addView(text("Recuperações: "+scores[0]+" · respostas completas: "+scores[1],13,WHITE,false));space(learning,6);
            learning.addView(text("Revisões previstas: "+ChallengeData.dueCount(recalls(),c.optString("id")),13,MUTED,false));space(learning,13);
            action(learning,"Registrar compreensão",false,()->{pendingLearningSession="";navigate("learningCapture");});space(learning,9);
            action(learning,"+  Cadastrar questão de recuperação",false,()->addRecall(c));space(learning,9);
            action(learning,"Abrir revisões espaçadas",false,()->navigate("reviews"));
            b.addView(learning);space(b,14);
            renderRecentEntries(b,c.optString("id"));
        }
        overline(b,"Últimos dias");
        int until=Math.min(c.optInt("days")-1,Math.max(0,idx));
        for(int i=until;i>=Math.max(0,until-6);i--){
            String iso=ChallengeData.shift(c.optString("startDate"),i);
            if(iso.compareTo(today)>0)continue;
            int goal=ChallengeData.goal(c,iso);
            long focus=ChallengeData.focusOn(sessions(),c.optString("id"),iso);
            LinearLayout line=card();
            line.addView(text(ChallengeData.dateLabel(iso)+" · "+(goal==0?"Descanso":human(focus)+" / "+goal+" min"),13,WHITE,true));
            space(line,5);
            line.addView(text(goal==0?"Sem meta":focus>=goal*60000L?"Meta cumprida":"Meta ainda não cumprida",12,goal>0&&focus>=goal*60000L?ACCENT:MUTED,false));
            b.addView(line);space(b,8);
        }
        space(b,12);action(b,"←  Todos os desafios",false,()->navigate("challenges"));
    }
    private void startChallenge(JSONObject c) {
        if(data.optJSONObject("timer")!=null){notice("Conclua ou descarte a sessão atual primeiro.");navigate("timer");return;}
        JSONObject a=ChallengeData.find(activities(),c.optString("activityId"));
        JSONObject t=a==null?null:ChallengeData.find(a.optJSONArray("types"),c.optString("typeId"));
        if(a==null||t==null){notice("A atividade ou tipo vinculado foi excluído.");return;}
        int goal=ChallengeData.goal(c,ChallengeData.today());
        if(goal<1){notice("Hoje está marcado como descanso.");return;}
        beginPreparation(a,t,goal);
        JSONObject timer=data.optJSONObject("timer");
        if(timer!=null){set(timer,"challengeId",c.optString("id"));save();}
    }
    private void chooseDayGoal(JSONObject c) {
        int days=c.optInt("days");String[] labels=new String[days];
        for(int i=0;i<days;i++) {
            String iso=ChallengeData.shift(c.optString("startDate"),i);
            labels[i]="Dia "+(i+1)+" · "+ChallengeData.dateLabel(iso)+" · "+ChallengeData.goal(c,iso)+" min";
        }
        new AlertDialog.Builder(this).setTitle("Selecionar dia")
            .setItems(labels,(d,i)->editDayGoal(c,ChallengeData.shift(c.optString("startDate"),i)))
            .setNegativeButton("Cancelar",null).show();
    }
    private void editDayGoal(JSONObject c,String iso) {
        EditText f=numberField("Minutos (0 = descanso)",String.valueOf(ChallengeData.goal(c,iso)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Meta · "+ChallengeData.dateLabel(iso))
            .setMessage("Informe 0 para marcar um dia de descanso.")
            .setView(dialogFields(f)).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int n=parseMinutes(f,0,720);if(n<0)return;
            JSONObject overrides=c.optJSONObject("dateGoals");
            if(overrides==null){overrides=new JSONObject();set(c,"dateGoals",overrides);}
            set(overrides,iso,n);save();d.dismiss();render();
        }));
        d.show();
    }
    private void changeDefaultGoal(JSONObject c) {
        EditText f=numberField("Minutos diários",String.valueOf(c.optInt("goalMinutes",40)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Meta diária padrão")
            .setView(dialogFields(f)).setPositiveButton("Salvar",null)
            .setNegativeButton("Cancelar",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int n=parseMinutes(f,1,720);if(n<0)return;
            set(c,"goalMinutes",n);save();d.dismiss();render();
        }));
        d.show();
    }
    private void renderRecentEntries(LinearLayout b,String challengeId) {
        overline(b,"Registros de compreensão");int shown=0;
        for(int i=entries().length()-1;i>=0&&shown<5;i--) {
            JSONObject entry=entries().optJSONObject(i);
            if(entry==null||!challengeId.equals(entry.optString("challengeId")))continue;
            shown++;LinearLayout tile=card();
            tile.addView(text(entry.optString("topic"),15,WHITE,true));space(tile,8);
            tile.addView(text(entry.optString("explanation"),13,MUTED,false));space(tile,5);
            tile.addView(text(ChallengeData.dateLabel(ChallengeData.date(entry.optLong("createdAt"))),11,ACCENT,false));
            b.addView(tile);space(b,8);
        }
        if(shown==0)b.addView(text("Nenhum registro de compreensão até agora.",13,MUTED,false));
        space(b,14);
    }
    private void renderLearningCapture(LinearLayout b) {
        JSONObject c=currentChallenge();
        if(c==null){navigate("challenges");return;}
        overline(b,"Registro autoral");
        section(b,"Compreensão","Escreva sem consultar o material. Registrar não equivale a comprovar retenção.");
        EditText topic=field("Tema ou trecho estudado","",false);
        EditText ideas=field("Ideias principais lembradas","",true);
        EditText explain=field("Explique com suas palavras, sem consultar","",true);
        EditText doubts=field("Dúvidas ou erros identificados","",true);
        LinearLayout panel=card();
        for(EditText f:new EditText[]{topic,ideas,explain,doubts}){panel.addView(f,params(-1,-2));space(panel,12);}
        b.addView(panel);space(b,13);
        action(b,"Salvar registro",true,()->{
            String title=clean(topic.getText().toString());
            String content=clean(explain.getText().toString());
            if(title.length()<2){topic.setError("Informe o tema.");return;}
            if(content.length()<10){explain.setError("Explique em pelo menos 10 caracteres.");return;}
            entries().put(ChallengeData.newEntry(c.optString("id"),pendingLearningSession,title,
                ideas.getText().toString().trim(),content,doubts.getText().toString().trim(),0));
            pendingLearningSession="";save();navigate("challengeDetail");notice("Compreensão registrada.");
        });space(b,12);
        action(b,"Agora não",false,()->{pendingLearningSession="";navigate("challengeDetail");});
    }
    private void addRecall(JSONObject c) {
        EditText question=field("Pergunta de recuperação", "",true);
        EditText reference=field("Resposta de referência (opcional)","",true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Nova questão")
            .setView(dialogFields(question,reference)).setNegativeButton("Cancelar",null)
            .setPositiveButton("Criar",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String q=clean(question.getText().toString());
            if(q.length()<8){question.setError("Digite uma pergunta específica.");return;}
            recalls().put(ChallengeData.newRecall(c.optString("id"),q,reference.getText().toString().trim()));
            save();dialog.dismiss();notice("Revisão agendada para amanhã.");render();
        }));
        dialog.show();
    }
    private void renderReviews(LinearLayout b) {
        overline(b,"Revisão espaçada");
        section(b,"Recuperação ativa","Responda de memória antes de conferir o material.");
        String id=selectedChallenge;
        int due=ChallengeData.dueCount(recalls(),id.isEmpty()?null:id);
        b.addView(text(due+" item(ns) previstos ou atrasados",16,ACCENT,true));space(b,16);
        int shown=0;String today=ChallengeData.today();
        for(int i=0;i<recalls().length();i++){
            JSONObject r=recalls().optJSONObject(i);
            if(r==null||(!id.isEmpty()&&!id.equals(r.optString("challengeId"))))continue;
            boolean scheduled=r.optString("dueDate","9999-12-31").compareTo(today)<=0;
            LinearLayout tile=card();
            tile.addView(text(r.optString("question"),15,WHITE,true));space(tile,7);
            tile.addView(text((scheduled?"Pendente":"Próxima")+" · "+ChallengeData.dateLabel(r.optString("dueDate")),12,scheduled?ACCENT:MUTED,false));space(tile,11);
            action(tile,scheduled?"Responder sem consultar":"Revisar antecipadamente",scheduled,()->reviewItem(r));
            b.addView(tile);space(b,11);shown++;
        }
        if(shown==0)b.addView(text("Nenhuma questão cadastrada ainda.",13,MUTED,false));
        space(b,12);
        action(b,"←  Voltar",false,()->navigate(id.isEmpty()?"challenges":"challengeDetail"));
    }
    private void reviewItem(JSONObject item) {
        EditText answer=field("Escreva tudo que lembra, sem consultar","",true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(item.optString("question"))
            .setView(dialogFields(answer)).setNegativeButton("Cancelar",null)
            .setPositiveButton("Conferir resposta",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String written=answer.getText().toString().trim();
            if(written.length()<2){answer.setError("Responda antes de conferir.");return;}
            dialog.dismiss();
            String reference=item.optString("expected","");
            String comparison="Sua resposta:\n"+written+"\n\n";
            comparison+=reference.isEmpty()?"Confira o material original antes de avaliar.":"Referência:\n"+reference;
            new AlertDialog.Builder(this).setTitle("Avalie a recuperação")
                .setMessage(comparison)
                .setItems(new String[]{"0 · Não recordei","1 · Recordei parcialmente","2 · Recordei corretamente"},
                    (d,which)->{
                        ChallengeData.recordRecall(data,item,written,which);save();render();
                        notice("Avaliação salva. Próxima revisão agendada.");
                    }).setNegativeButton("Não avaliar",null).show();
        }));
        dialog.show();
    }
    private void exportBackup() {
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE,"1o-backup-"+ChallengeData.today()+".json");
        startActivityForResult(i,BACKUP_EXPORT);
    }
    private void importBackup() {
        new AlertDialog.Builder(this).setTitle("Restaurar dados?")
            .setMessage("A restauração substituirá os registros deste aparelho. Exporte uma cópia antes de continuar.")
            .setPositiveButton("Escolher arquivo",(d,w)->{
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
                startActivityForResult(i,BACKUP_IMPORT);
            }).setNegativeButton("Cancelar",null).show();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent result) {
        super.onActivityResult(requestCode,resultCode,result);
        if(resultCode!=RESULT_OK||result==null||result.getData()==null)return;
        Uri uri=result.getData();
        try {
            if(requestCode==BACKUP_EXPORT) {
                try(OutputStream out=getContentResolver().openOutputStream(uri)) {
                    if(out==null)throw new IllegalStateException("Sem acesso ao arquivo");
                    out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));out.flush();
                }
                notice("Cópia exportada.");
            } else if(requestCode==BACKUP_IMPORT) {
                ByteArrayOutputStream mem=new ByteArrayOutputStream();
                try(InputStream in=getContentResolver().openInputStream(uri)) {
                    if(in==null)throw new IllegalStateException("Sem leitura");
                    byte[] buffer=new byte[8192];int n;
                    while((n=in.read(buffer))!=-1) {
                        if(mem.size()+n>8*1024*1024)throw new IllegalStateException("Arquivo excede 8 MB");
                        mem.write(buffer,0,n);
                    }
                }
                JSONObject restored=new JSONObject(new String(mem.toByteArray(),StandardCharsets.UTF_8));
                if(restored.optJSONArray("activities")==null||restored.optJSONArray("sessions")==null)
                    throw new IllegalArgumentException("Não é um backup compatível");
                ChallengeData.migrate(restored);
                data=restored;save();selectedChallenge="";pendingLearningSession="";navigate("challenges");
                notice("Dados restaurados.");
            }
        } catch(Exception e) {notice("Falha ao acessar backup: "+e.getMessage());}
    }

    private void editActivity(JSONObject existing) {
        boolean isEdit=existing!=null;
        prompt(isEdit?"Renomear atividade":"Cadastrar atividade",
                "Nome da atividade (2 a 36 caracteres)",isEdit?existing.optString("name"):"",name->{
            if(duplicate(activities(),name,isEdit?existing.optString("id"):"")){
                notice("Já existe uma atividade com este nome.");return;
            }
            if(isEdit) set(existing,"name",name);
            else {
                JSONObject a=activity(name);activities().put(a);
                selectedActivity=a.optString("id");page="detail";
            }
            save();render();notice("Atividade salva.");
        });
    }
    private void editType(JSONObject existing) {
        JSONObject a=chosen();if(a==null)return;
        JSONArray types=a.optJSONArray("types");boolean isEdit=existing!=null;
        prompt(isEdit?"Renomear tipo":"Cadastrar tipo",
                "Nome do tipo (2 a 36 caracteres)",isEdit?existing.optString("name"):"",name->{
            if(duplicate(types,name,isEdit?existing.optString("id"):"")){
                notice("Este tipo já existe na atividade.");return;
            }
            if(isEdit)set(existing,"name",name);else types.put(record(name));
            save();render();notice("Tipo salvo.");
        });
    }
    private interface SaveName { void save(String name); }
    private void prompt(String title,String hint,String current,SaveName callback) {
        EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(current);input.setSelection(input.getText().length());
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(36)});
        input.setHint(hint);
        LinearLayout wrapper=column(); wrapper.setPadding(dp(21),dp(7),dp(21),0);wrapper.addView(input,params(-1,-2));
        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle(title).setView(wrapper).setNegativeButton("Cancelar",(d,w)->{})
                .setPositiveButton("Salvar",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String value=clean(input.getText().toString());
            if(value.length()<2||value.length()>36){input.setError("Utilize de 2 a 36 caracteres.");return;}
            callback.save(value);dialog.dismiss();
        }));
        dialog.show();
    }
    private void confirmDeleteActivity(JSONObject a) {
        new AlertDialog.Builder(this).setTitle("Excluir atividade?")
                .setMessage("A atividade e seus tipos serão excluídos. O histórico de sessões será preservado.")
                .setNegativeButton("Cancelar",(d,w)->{})
                .setPositiveButton("Excluir",(d,w)->{
                    set(data,"activities",remove(activities(),a.optString("id")));
                    selectedActivity="";save();navigate("activities");notice("Atividade excluída.");
                }).show();
    }
    private void confirmDeleteType(JSONObject t) {
        new AlertDialog.Builder(this).setTitle("Excluir tipo?")
                .setMessage("O tipo será removido. As sessões anteriores permanecerão no histórico.")
                .setNegativeButton("Cancelar",(d,w)->{})
                .setPositiveButton("Excluir",(d,w)->{
                    JSONObject a=chosen();if(a!=null){set(a,"types",remove(a.optJSONArray("types"),t.optString("id")));save();render();}
                }).show();
    }
    private void start(JSONObject a,JSONObject type) {
        if(data.optJSONObject("timer")!=null){
            new AlertDialog.Builder(this).setTitle("Há uma sessão em andamento")
                .setMessage("Conclua ou descarte a sessão atual antes de iniciar outra.")
                .setPositiveButton("Abrir sessão",(d,w)->navigate("timer"))
                .setNegativeButton("Cancelar",(d,w)->{}).show();
            return;
        }
        EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);input.setText("20");input.setSelection(input.length());
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        LinearLayout wrapper=column();wrapper.setPadding(dp(24),dp(5),dp(24),0);
        wrapper.addView(text("Minutos planejados para o foco (1 a 720). A preparação não possui limite.",13,MUTED,false));
        space(wrapper,10);wrapper.addView(input,params(-1,-2));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Definir bloco de foco")
            .setView(wrapper).setNegativeButton("Cancelar",(d,w)->{})
            .setPositiveButton("Iniciar preparação",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int minutes;
            try {minutes=Integer.parseInt(input.getText().toString().trim());}
            catch(Exception e){input.setError("Informe os minutos.");return;}
            if(minutes<1||minutes>720){input.setError("Escolha entre 1 e 720 minutos.");return;}
            beginPreparation(a,type,minutes);dialog.dismiss();
        }));
        dialog.show();
    }
    private void beginPreparation(JSONObject a,JSONObject type,int minutes) {
        JSONObject timer=new JSONObject();long now=System.currentTimeMillis();
        set(timer,"activityName",a.optString("name"));set(timer,"typeName",type.optString("name"));
        set(timer,"activityId",a.optString("id"));set(timer,"typeId",type.optString("id"));
        set(timer,"phase","preparation");set(timer,"plannedMillis",minutes*60000L);
        set(timer,"prepMillis",0L);set(timer,"savedMillis",0L);
        set(timer,"startedAt",now);set(timer,"createdAt",now);set(timer,"paused",false);
        set(data,"timer",timer);save();navigate("timer");
    }
    private void beginFocus() {
        JSONObject t=data.optJSONObject("timer");if(t==null||!isPreparation(t))return;
        long now=System.currentTimeMillis();
        set(t,"prepMillis",elapsed());
        set(t,"phase","focus");set(t,"savedMillis",0L);
        set(t,"startedAt",now);set(t,"focusStartedAt",now);
        set(t,"paused",false);save();render();
    }
    private void toggleTimer() {
        JSONObject t=data.optJSONObject("timer");if(t==null)return;
        if(t.optBoolean("paused",false)) {
            set(t,"paused",false);set(t,"startedAt",System.currentTimeMillis());
        } else {
            set(t,"savedMillis",elapsed());set(t,"paused",true);
        }
        save();render();
    }
    private void finishTimer() {
        JSONObject t=data.optJSONObject("timer");if(t==null)return;
        if(isPreparation(t)) {
            new AlertDialog.Builder(this).setTitle("Encerrar antes do foco?")
                .setMessage("O tempo de preparação será salvo, e a sessão ficará identificada como não iniciada.")
                .setNegativeButton("Continuar preparação",(d,w)->{})
                .setPositiveButton("Registrar e encerrar",(d,w)->completeTimer(false)).show();
        } else completeTimer(true);
    }
    private void completeTimer(boolean completed) {
        JSONObject t=data.optJSONObject("timer");if(t==null)return;
        long prep=preparationMillis(t),focus=completed?focusMillis(t):0L,planned=t.optLong("plannedMillis",0L);
        JSONObject s=new JSONObject();
        set(s,"schemaVersion",2);set(s,"id",UUID.randomUUID().toString());
        set(s,"activityName",t.optString("activityName"));set(s,"typeName",t.optString("typeName"));
        set(s,"activityId",t.optString("activityId"));set(s,"typeId",t.optString("typeId"));
        set(s,"prepMillis",prep);set(s,"focusMillis",focus);
        set(s,"plannedMillis",planned);set(s,"extraMillis",planned>0?Math.max(0L,focus-planned):0L);
        set(s,"status",completed?"completed":"preparation_only");
        set(s,"createdAt",t.optLong("createdAt",0L));
        set(s,"focusStartedAt",t.optLong("focusStartedAt",0L));
        set(s,"millis",focus);set(s,"finishedAt",System.currentTimeMillis());
        String challengeId=t.optString("challengeId","");
        if(!challengeId.isEmpty())set(s,"challengeId",challengeId);
        sessions().put(s);data.remove("timer");save();
        JSONObject challenge=ChallengeData.find(challenges(),challengeId);
        if(completed && challenge!=null && "learning".equals(challenge.optString("mode"))){
            selectedChallenge=challengeId;pendingLearningSession=s.optString("id");navigate("learningCapture");
        }else navigate("history");
        notice("Registro salvo no histórico.");
    }
    private void confirmDiscard() {
        new AlertDialog.Builder(this).setTitle("Descartar sessão?")
                .setMessage("O tempo desta sessão não será registrado.")
                .setNegativeButton("Cancelar",(d,w)->{})
                .setPositiveButton("Descartar",(d,w)->{data.remove("timer");save();navigate("home");}).show();
    }
    private void notice(String message) {Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
}
