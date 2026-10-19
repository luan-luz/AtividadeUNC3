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

/** 1o v1.1: aplicativo Android nativo offline; preparação, foco planejado e excedente. */
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
        }
        render();
        handler.post(tick);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("page", page);
        out.putString("activity", selectedActivity);
        super.onSaveInstanceState(out);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if ("timer".equals(page) || "detail".equals(page)) navigate("activities");
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
            default: renderHome(body); break;
        }
        if (!"timer".equals(page)) shell.addView(nav(), params(-1, 71));
        setContentView(shell);
    }
    private LinearLayout nav() {
        LinearLayout nav = row();nav.setPadding(dp(8),dp(5),dp(8),dp(5));
        nav.setBackgroundColor(SURFACE);nav.setGravity(Gravity.CENTER);nav.setWeightSum(3f);
        String[] ids = {"home", "activities", "history"};
        String[] labels = {"Início", "Atividades", "Histórico"};
        String[] icons = {"⌂", "☷", "◷"};
        for (int i=0; i<3; i++) {
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
        b.addView(start,params(-1,-2)); space(b,24);
        overline(b,"Suas atividades");
        if (activities().length()==0) b.addView(text("Nenhuma atividade cadastrada.",14,MUTED,false));
        for (int i=0;i<Math.min(activities().length(),4);i++) {
            JSONObject a=activities().optJSONObject(i); if (a!=null) activityLine(b,a);
        }
        space(b,14); action(b,"+  Nova atividade",false,()->editActivity(null));
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
            c.addView(text(s.optString("typeName"),13,MUTED,false));space(c,12);
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
        sessions().put(s);data.remove("timer");save();navigate("history");
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
