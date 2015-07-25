package barqsoft.footballscores.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.ScoresDBHelper;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.scoresAdapter;

/**
 * @author Pierce
 * @since 7/25/2015.
 */
public class MatchWidgetService extends IntentService {

    public MatchWidgetService() {
        super(MatchWidgetService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());

        int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

        if(allWidgetIds == null || allWidgetIds.length == 0){
            return;
        }

        // We just want to get the next match
        Cursor cursor = new ScoresDBHelper(this).getReadableDatabase().query(DatabaseContract.SCORES_TABLE,
                null,
                null,
                null,
                null,
                null,
                DatabaseContract.scores_table.DATE_TIME_COL + " DESC");

        String homeTeam = "";
        String awayTeam = "";
        String matchDate = "";
        String score = "";

        boolean matchExists = false;
        if (cursor.moveToNext()) {
            matchExists = true;
            homeTeam = cursor.getString(scoresAdapter.COL_HOME);
            awayTeam = cursor.getString(scoresAdapter.COL_AWAY);

            matchDate = cursor.getString(scoresAdapter.COL_MATCHTIME);

            score = Utilies.getScores(cursor.getInt(scoresAdapter.COL_HOME_GOALS), cursor.getInt(scoresAdapter.COL_AWAY_GOALS));
        }

        cursor.close();

        for (int widgetId : allWidgetIds) {

            RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.match_widget);
            setupViews(remoteViews, matchExists, homeTeam, awayTeam, matchDate, score);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private void setupViews(final RemoteViews remoteViews, boolean show,
                            String homeTeam, String awayTeam, String matchDate, String score) {

        remoteViews.setViewVisibility(R.id.widget_container, show ? View.VISIBLE : View.GONE);
        remoteViews.setViewVisibility(R.id.widget_empty_textview, show ? View.GONE : View.VISIBLE);

        if (show) {
            remoteViews.setTextViewText(R.id.widget_score_textview, score);
            remoteViews.setContentDescription(R.id.widget_score_textview, score);

            remoteViews.setTextViewText(R.id.widget_date_textview, matchDate);
            remoteViews.setContentDescription(R.id.widget_date_textview, matchDate);

            remoteViews.setTextViewText(R.id.widget_home_name, homeTeam);
            remoteViews.setContentDescription(R.id.widget_home_name, homeTeam);
            remoteViews.setImageViewResource(R.id.widget_home_crest, Utilies.getTeamCrestByTeamName(homeTeam));
            remoteViews.setContentDescription(R.id.widget_home_crest, homeTeam);

            remoteViews.setTextViewText(R.id.widget_away_name, awayTeam);
            remoteViews.setContentDescription(R.id.widget_away_name, awayTeam);
            remoteViews.setImageViewResource(R.id.widget_away_crest, Utilies.getTeamCrestByTeamName(awayTeam));
            remoteViews.setContentDescription(R.id.widget_away_crest, awayTeam);
        }
    }


    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }
}
