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

/** TRILHA 1.0: aplicativo Android nativo, sem WebView, sem internet, sem dependências de terceiros. */
public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(24, 38, 36);
    private static final int MUTED = Color.rgb(98, 112, 106);
    private static final int GREEN = Color.rgb(36, 91, 80);
    private static final int BG = Color.rgb(247, 249, 246);
    private static final int WHITE = Color.WHITE;
    private static final int LINE = Color.rgb(226, 233, 227);
    private static final int SOFT = Color.rgb(235, 245, 231);
    private static final int DANGER = Color.rgb(158, 58, 60);

    private JSONObject data;
    private String page = "home";
    private String selectedActivity = "";
    private TextView clockView;
    private TextView clockStatus;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if ("timer".equals(page) && clockView != null) {
                clockView.setText(clockFormat(elapsed()));
                JSONObject timer = data.optJSONObject("timer");
                if (clockStatus != null) clockStatus.setText(timer != null && timer.optBoolean("paused", false) ? "Pausado" : "Em andamento");
            }
            handler.postDelayed(this, 250);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
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
        c.setBackground(shape(WHITE, 20, LINE)); return c;
    }
    private void action(LinearLayout container, String label, boolean primary, Runnable click) {
        TextView b = text(label, 14, primary ? WHITE : GREEN, true);
        b.setGravity(Gravity.CENTER); b.setMinHeight(dp(49));
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        b.setBackground(shape(primary ? GREEN : SOFT, 14, primary ? 0 : LINE));
        b.setOnClickListener(v -> click.run());
        b.setFocusable(true); b.setClickable(true);
        container.addView(b, params(-1, -2));
    }
    private TextView smallAction(String label, Runnable click, int color) {
        TextView t = text(label, 12, color, true);
        t.setPadding(dp(11), dp(9), dp(11), dp(9)); t.setGravity(Gravity.CENTER);
        t.setBackground(shape(color == DANGER ? 0xFFFCEEEE : SOFT, 10, 0));
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
        TextView t = text(value.toUpperCase(new Locale("pt", "BR")), 11, GREEN, true);
        t.setLetterSpacing(0.13f); body.addView(t); space(body, 9);
    }
    private void separator(LinearLayout target) {
        View line = new View(this); line.setBackgroundColor(LINE);
        LinearLayout.LayoutParams p = params(-1, 1); p.topMargin=dp(11);p.bottomMargin=dp(11);
        target.addView(line, p);
    }
    private void navigate(String dest) { page = dest; render(); }
    private void render() {
        clockView = null; clockStatus = null;
        LinearLayout shell = column(); shell.setBackgroundColor(BG);
        LinearLayout top = row(); top.setPadding(dp(22), dp(12), dp(22), dp(12));
        TextView brand = text("◉  TRILHA", 20, GREEN, true); top.addView(brand, params(-2, 36));
        TextView subtitle = text("MEU TEMPO, MEU RITMO", 9, MUTED, true);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(36), 1);
        subtitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); top.addView(subtitle, right);
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
        LinearLayout nav = row(); nav.setPadding(dp(13), dp(5), dp(13), dp(5)); nav.setBackgroundColor(WHITE);
        String[] ids = {"home", "activities", "history"};
        String[] labels = {"Início", "Atividades", "Histórico"};
        String[] icons = {"⌂", "☷", "◷"};
        for (int i=0; i<3; i++) {
            final String dest=ids[i];
            LinearLayout item=column(); item.setGravity(Gravity.CENTER);
            TextView ic=text(icons[i], 25, page.equals(dest)||("detail".equals(page)&&"activities".equals(dest))?GREEN:MUTED, true);
            TextView label=text(labels[i], 11, page.equals(dest)||("detail".equals(page)&&"activities".equals(dest))?GREEN:MUTED, true);
            item.addView(ic); item.addView(label);
            item.setOnClickListener(v->navigate(dest)); item.setFocusable(true); item.setClickable(true);
            nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));
        }
        return nav;
    }

    private long elapsed() {
        JSONObject t = data.optJSONObject("timer"); if (t == null) return 0;
        long millis = t.optLong("savedMillis", 0);
        if (!t.optBoolean("paused", false)) millis += Math.max(0, System.currentTimeMillis() - t.optLong("startedAt", System.currentTimeMillis()));
        return Math.max(0, millis);
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
        overline(b, "Organize. Concentre. Conclua.");
        section(b, "Bom momento para focar.", "Uma tarefa por vez. Escolha o que deseja fazer e acompanhe seu progresso.");
        long sum=0; int count=0;
        for (int i=0;i<sessions().length();i++) {
            JSONObject s = sessions().optJSONObject(i);
            if (s!=null && today(s.optLong("finishedAt",0))) {sum += s.optLong("millis",0); count++;}
        }
        LinearLayout metric=column(); metric.setPadding(dp(22),dp(22),dp(22),dp(21)); metric.setBackground(shape(GREEN,24,0));
        metric.addView(text("Tempo de foco hoje",13,0xFFDFEDE5,false)); space(metric,6);
        metric.addView(text(human(sum),39,WHITE,true)); space(metric,9);
        metric.addView(text(count+" sessão(ões) concluída(s)",12,0xFFDCEDE3,false));
        b.addView(metric, params(-1,-2)); space(b,15);
        LinearLayout start=card(); start.addView(text("Comece uma sessão",18,INK,true)); space(start,8);
        start.addView(text("Selecione uma atividade e um tipo. O cronômetro ficará salvo mesmo se você sair do aplicativo.",13,MUTED,false)); space(start,16);
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
        top.addView(smallAction("Abrir  →",()->{selectedActivity=a.optString("id");navigate("detail");},GREEN));
        line.addView(top); b.addView(line,params(-1,-2)); space(b,10);
    }
    private void renderActivities(LinearLayout b) {
        overline(b,"Biblioteca pessoal");
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
        section(b,a.optString("name"),"Cada tipo pode ter sessões de foco independentes.");
        LinearLayout actions=row();
        actions.addView(smallAction("Renomear",()->editActivity(a),GREEN));
        spaceHorizontal(actions,10);
        actions.addView(smallAction("Excluir",()->confirmDeleteActivity(a),DANGER));
        b.addView(actions);space(b,25);
        overline(b,"Tipos cadastrados");
        JSONArray types=a.optJSONArray("types");
        if (types!=null) for(int i=0;i<types.length();i++) {
            JSONObject t=types.optJSONObject(i); if (t==null) continue;
            LinearLayout c=card();c.addView(text(t.optString("name"),16,INK,true)); space(c,12);
            LinearLayout r=row(); r.addView(smallAction("▶  Iniciar",()->start(a,t),GREEN));
            spaceHorizontal(r,8); r.addView(smallAction("Editar",()->editType(t),GREEN));
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
        overline(b,"Sessão de concentração");
        section(b,t.optString("activityName"),t.optString("typeName"));
        space(b,26);
        LinearLayout circle=column();circle.setGravity(Gravity.CENTER);
        GradientDrawable outline=shape(WHITE,160,GREEN);outline.setShape(GradientDrawable.OVAL);outline.setStroke(dp(9),0xFFA3C7B1);
        circle.setBackground(outline);
        clockView=text(clockFormat(elapsed()),34,INK,true);clockView.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
        circle.addView(clockView);
        clockStatus=text(t.optBoolean("paused",false)?"Pausado":"Em andamento",13,MUTED,false);
        space(circle,7);circle.addView(clockStatus);
        LinearLayout.LayoutParams cp=params(285,285);cp.gravity=Gravity.CENTER_HORIZONTAL;
        b.addView(circle,cp); space(b,28);
        TextView msg=text("Concentre-se em uma atividade por vez. Você pode pausar e retomar quando precisar.",13,MUTED,false);
        msg.setGravity(Gravity.CENTER);b.addView(msg);space(b,28);
        action(b,t.optBoolean("paused",false)?"▶  Retomar cronômetro":"Ⅱ  Pausar cronômetro",true,()->toggleTimer());
        space(b,11);action(b,"✓  Concluir e salvar sessão",false,()->finishTimer());
        space(b,13);action(b,"Voltar sem encerrar",false,()->navigate("home"));
        space(b,13);
        TextView discard=text("Descartar esta sessão",13,DANGER,true);discard.setGravity(Gravity.CENTER);
        discard.setPadding(0,dp(15),0,dp(15));discard.setOnClickListener(v->confirmDiscard());
        b.addView(discard);
    }
    private void renderHistory(LinearLayout b) {
        overline(b,"Seu progresso");
        section(b,"Histórico de foco","Sessões concluídas e salvas no próprio aparelho.");
        long sum=0;for(int i=0;i<sessions().length();i++) {
            JSONObject s=sessions().optJSONObject(i);if(s!=null)sum+=s.optLong("millis",0);
        }
        LinearLayout metric=card(); metric.addView(text("Tempo acumulado",12,MUTED,true));space(metric,6);
        metric.addView(text(human(sum),30,GREEN,true));space(metric,5);
        metric.addView(text(sessions().length()+" sessões concluídas",12,MUTED,false));
        b.addView(metric,params(-1,-2));space(b,21);
        if(sessions().length()==0) {
            b.addView(text("Você ainda não concluiu sessões. Inicie uma atividade para registrar seu progresso.",13,MUTED,false));
        }
        for(int i=sessions().length()-1;i>=0;i--) {
            JSONObject s=sessions().optJSONObject(i);if(s==null)continue;
            LinearLayout c=card();
            c.addView(text(s.optString("activityName"),17,INK,true));space(c,4);
            c.addView(text(s.optString("typeName"),13,MUTED,false));space(c,13);
            LinearLayout r=row();
            TextView date=text(day(s.optLong("finishedAt",0)),12,MUTED,false);r.addView(date,new LinearLayout.LayoutParams(0,-2,1));
            r.addView(text(human(s.optLong("millis",0)),14,GREEN,true));c.addView(r);
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
                    .setPositiveButton("Abrir cronômetro",(d,w)->navigate("timer"))
                    .setNegativeButton("Cancelar",(d,w)->{}).show();return;
        }
        JSONObject timer=new JSONObject();
        set(timer,"activityName",a.optString("name"));set(timer,"typeName",type.optString("name"));
        set(timer,"activityId",a.optString("id"));set(timer,"typeId",type.optString("id"));
        set(timer,"savedMillis",0L);set(timer,"startedAt",System.currentTimeMillis());set(timer,"paused",false);
        set(data,"timer",timer);save();navigate("timer");
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
        long ms=elapsed();if(ms<1000){notice("Espere pelo menos um segundo para concluir.");return;}
        JSONObject s=new JSONObject();set(s,"id",UUID.randomUUID().toString());
        set(s,"activityName",t.optString("activityName"));set(s,"typeName",t.optString("typeName"));
        set(s,"activityId",t.optString("activityId"));set(s,"typeId",t.optString("typeId"));
        set(s,"millis",ms);set(s,"finishedAt",System.currentTimeMillis());
        sessions().put(s);data.remove("timer");save();navigate("history");notice("Sessão salva no histórico.");
    }
    private void confirmDiscard() {
        new AlertDialog.Builder(this).setTitle("Descartar sessão?")
                .setMessage("O tempo desta sessão não será registrado.")
                .setNegativeButton("Cancelar",(d,w)->{})
                .setPositiveButton("Descartar",(d,w)->{data.remove("timer");save();navigate("home");}).show();
    }
    private void notice(String message) {Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
}
