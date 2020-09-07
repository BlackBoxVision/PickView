package io.blackbox_vision.wheelview.view;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.blackbox_vision.wheelview.R;
import io.blackbox_vision.wheelview.utils.DateUtils;

import static io.blackbox_vision.wheelview.utils.DateUtils.formatDate;


public final class DatePickerWheelView extends LinearLayout {
    private static final String TAG = DatePickerWheelView.class.getSimpleName();

    private static final String LONG_MONTH_FORMAT = "MMMM";
    private static final String SHORT_MONTH_FORMAT = "MMM";
    private static final String YEAR_FORMAT = "yyyy";
    private static final String MONTH_FORMAT = "MM";
    private static final String DAY_FORMAT = "dd";

    private WheelView yearSpinner;
    private WheelView monthSpinner;
    private WheelView daySpinner;

    private float textSize;

    private int minYear;
    private int maxYear;

    private int lineColor;

    private int overflowTextColor;
    private int contentTextColor;

    private String initialDate;

    private boolean showDayMonthYear;
    private boolean showShortMonths;
    private boolean showLongMonths;

    @NonNull
    private final List<String> years = new ArrayList<>();

    @NonNull
    private final List<String> months = new ArrayList<>();

    @NonNull
    private final List<String> days = new ArrayList<>();

    private int yearPos = 0;
    private int monthPos = 0;
    private int dayPos = 0;

    @Nullable
    private OnDateSelectedListener onDateSelectedListener;

    private Calendar calendar;

    @NonNull
    private Locale locale = Locale.getDefault();

    private View rootView;
    private String fontTypeface;
    private boolean canLoop;

    public DatePickerWheelView(Context context) {
        this(context, null, 0);
    }

    public DatePickerWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DatePickerWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        takeStyles(attrs);
        drawDatePickerWheelView();
    }

    @TargetApi(21)
    public DatePickerWheelView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        takeStyles(attrs);
        drawDatePickerWheelView();
    }

    private void takeStyles(@NonNull AttributeSet attrs) {
        final TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.DatePickerWheelView);

        try {
            if (null != array) {
                overflowTextColor = array.getColor(R.styleable.DatePickerWheelView_datePickerWheelViewOverflowTextColor, 0xffafafaf);
                contentTextColor = array.getColor(R.styleable.WheelView_wheelViewContentTextColor, 0xff313131);
                lineColor = array.getColor(R.styleable.DatePickerWheelView_datePickerWheelViewLineColor, 0xffc5c5c5);

                showDayMonthYear = array.getBoolean(R.styleable.DatePickerWheelView_datePickerWheelViewShowDayMonthYear, false);
                showShortMonths = array.getBoolean(R.styleable.DatePickerWheelView_datePickerWheelViewShowShortMonths, false);
                showLongMonths = array.getBoolean(R.styleable.DatePickerWheelView_datePickerWheelViewShowLongMonths, false);

                textSize = array.getDimension(R.styleable.DatePickerWheelView_datePickerWheelViewTextSize, 16F);

                minYear = array.getInt(R.styleable.DatePickerWheelView_datePickerWheelViewMinYear, 1980);
                maxYear = array.getInt(R.styleable.DatePickerWheelView_datePickerWheelViewMaxYear, 2100);

                initialDate = array.getString(R.styleable.DatePickerWheelView_datePickerWheelViewInitialDate);
                fontTypeface = array.getString(R.styleable.DatePickerWheelView_datePickerWheelViewFontTypeface);
                canLoop = array.getBoolean(R.styleable.DatePickerWheelView_datePickerWheelViewIsLoopEnabled, false);
            }
        } finally {
            if (null != array) {
                array.recycle();
            }
        }
    }

    private void drawDatePickerWheelView() {
        rootView = LayoutInflater.from(getContext()).inflate(showDayMonthYear ? R.layout.date_picker_inverted : R.layout.date_picker, this, true);

        yearSpinner = (WheelView) rootView.findViewById(R.id.picker_year);
        monthSpinner = (WheelView) rootView.findViewById(R.id.picker_month);
        daySpinner = (WheelView) rootView.findViewById(R.id.picker_day);


        yearSpinner.setLineColor(lineColor);
        monthSpinner.setLineColor(lineColor);
        daySpinner.setLineColor(lineColor);

        yearSpinner.setContentTextColor(contentTextColor);
        monthSpinner.setContentTextColor(contentTextColor);
        daySpinner.setContentTextColor(contentTextColor);

        yearSpinner.setOverflowTextColor(overflowTextColor);
        monthSpinner.setOverflowTextColor(overflowTextColor);
        daySpinner.setOverflowTextColor(overflowTextColor);

        yearSpinner.setTextSize(textSize);
        monthSpinner.setTextSize(textSize);
        daySpinner.setTextSize(textSize);

        yearSpinner.setIsLoopEnabled(canLoop);
        monthSpinner.setIsLoopEnabled(canLoop);
        daySpinner.setIsLoopEnabled(canLoop);

        yearSpinner.addOnLoopScrollListener(this::updateYearPosition);
        yearSpinner.addOnLoopScrollListener(this::onDateSelected);

        monthSpinner.addOnLoopScrollListener(this::updateMonthPosition);
        monthSpinner.addOnLoopScrollListener(this::onDateSelected);

        daySpinner.addOnLoopScrollListener(this::updateDayPosition);
        daySpinner.addOnLoopScrollListener(this::onDateSelected);

        drawYearPickerView();
        drawMonthPickerView();
        drawDayPickerView();

        setInitialPositions();
        setFontTypeface(fontTypeface);
    }

    private void updateYearPosition(@NonNull Object item, int position) {
        this.yearPos = position;
        // Leap years have 29 days in February
        drawDayPickerView();
        invalidate();
    }

    private void updateMonthPosition(@NonNull Object item, int position) {
        this.monthPos = position;
        drawDayPickerView();
        invalidate();
    }

    private void updateDayPosition(@NonNull Object item, int position) {
        this.dayPos = position;
        invalidate();
    }

    private void onDateSelected(@NonNull Object item, int position) {
        if (null != onDateSelectedListener) {
            final Calendar calendar = Calendar.getInstance(locale);

            calendar.set(Calendar.YEAR, Integer.valueOf(years.get(yearPos)));
            calendar.set(Calendar.MONTH, monthPos);
            // Prevent an IndexOutOfBoundsException
            calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(days.get(Math.min(days.size() - 1, dayPos))));

            final int year = calendar.get(Calendar.YEAR);
            final int month = calendar.get(Calendar.MONTH);
            final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            onDateSelectedListener.onDateSelected(year, month, dayOfMonth);
        }
    }

    private void setInitialPositions() {
        final Calendar calendar = DateUtils.parseDateString(initialDate);

        yearPos = years.indexOf(String.valueOf(calendar.get(Calendar.YEAR)));
        String dateFormat = showShortMonths ? SHORT_MONTH_FORMAT : MONTH_FORMAT;
        dateFormat = showLongMonths ? LONG_MONTH_FORMAT : dateFormat;
        String formattedMonth = formatDate(calendar, locale, dateFormat);
        formattedMonth = showShortMonths ? formattedMonth.toUpperCase(locale) : formattedMonth;
        // Fixed: months array was accessed without custom format
        monthPos = months.indexOf(formattedMonth);
        // Fixed: days was accessed without "dd"
        dayPos = days.indexOf(formatDate(calendar, locale, DAY_FORMAT));

        yearSpinner.setInitialPosition(yearPos);
        monthSpinner.setInitialPosition(monthPos);
        daySpinner.setInitialPosition(dayPos);
    }

    private void drawYearPickerView() {
        deleteAll(years);
        calendar = Calendar.getInstance(locale);

        final int yearCount = maxYear - minYear;

        for (int i = 0; i < yearCount; i++) {
            calendar.set(Calendar.YEAR, minYear + i);

            years.add(i, formatDate(calendar, locale, YEAR_FORMAT));
        }

        yearSpinner.setItems(years);
    }

    private void drawMonthPickerView() {
        deleteAll(months);
        calendar = Calendar.getInstance(locale);

        String dateFormat = showShortMonths ? SHORT_MONTH_FORMAT : MONTH_FORMAT;
        // Support long months
        dateFormat = showLongMonths ? LONG_MONTH_FORMAT : dateFormat;

        for (int j = 0; j <= calendar.getActualMaximum(Calendar.MONTH); j++) {
            calendar.set(Calendar.YEAR, Integer.valueOf(years.get(yearPos)));
            calendar.set(Calendar.MONTH, j);
            calendar.set(Calendar.DAY_OF_MONTH, 1);

            String formattedMonth = formatDate(calendar, locale, dateFormat);
            formattedMonth = showShortMonths ? formattedMonth.toUpperCase(locale) : formattedMonth;

            months.add(j, formattedMonth);
        }

        monthSpinner.setItems(months);
    }

    private void drawDayPickerView() {
        deleteAll(days);
        final int year = Integer.valueOf(years.get(yearPos));
        //final int month = showShortMonths ? months.indexOf(months.get(monthPos)) + 1 : Integer.valueOf(months.get(monthPos));

        calendar = Calendar.getInstance(locale);

        // Fixed: dayPos while changing to a shorter month
        Calendar cl = Calendar.getInstance(locale);
        cl.set(Calendar.YEAR, year);
        cl.set(Calendar.DAY_OF_MONTH, 1);
        cl.set(Calendar.MONTH, monthPos);
        int actualMaximum = cl.getActualMaximum(Calendar.DAY_OF_MONTH);

        if (dayPos > actualMaximum - 1) {
            int delta = dayPos - actualMaximum + 1;
            dayPos = delta - 1;
            daySpinner.forceScroll(delta);
        }
        ///

        for (int i = 0; i < calendar.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthPos);
            calendar.set(Calendar.DAY_OF_MONTH, i + 1);

            days.add(i, formatDate(calendar, locale, DAY_FORMAT));
        }

        daySpinner.setItems(days);
    }

    private void deleteAll(@NonNull List list) {
        if (list.size() > 0) {
            for (Iterator iterator = list.listIterator(); iterator.hasNext(); ) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    public interface OnDateSelectedListener {

        void onDateSelected(int year, int month, int dayOfMonth);
    }

    public DatePickerWheelView setOnDateSelectedListener(@Nullable OnDateSelectedListener onDateSelectedListener) {
        this.onDateSelectedListener = onDateSelectedListener;
        invalidate();
        return this;
    }

    public DatePickerWheelView setLocale(@NonNull Locale locale) {
        this.locale = locale;
        invalidate();
        return this;
    }

    public DatePickerWheelView setMaxYear(int maxYear) {
        if (minYear > maxYear) {
            throw new IllegalArgumentException("minYear can't be major to maxYear");
        }

        this.maxYear = maxYear;
        drawYearPickerView();
        invalidate();
        return this;
    }

    public DatePickerWheelView setMinYear(int minYear) {
        this.minYear = minYear;
        invalidate();
        return this;
    }

    public DatePickerWheelView setShowDayMonthYear(boolean showDayMonthYear) {
        this.showDayMonthYear = showDayMonthYear;
        invalidate();
        return this;
    }

    public DatePickerWheelView setShowShortMonths(boolean showShortMonths) {
        this.showShortMonths = showShortMonths;
        drawMonthPickerView();
        invalidate();
        return this;
    }

    public DatePickerWheelView setShowLongMonths(boolean showLongMonths) {
        this.showLongMonths = showLongMonths;
        drawMonthPickerView();
        invalidate();
        return this;
    }

    public DatePickerWheelView setTextSize(float textSize) {
        this.textSize = textSize;
        invalidate();
        return this;
    }

    public DatePickerWheelView setLineColor(int lineColor) {
        this.lineColor = lineColor;
        invalidate();
        return this;
    }

    public DatePickerWheelView setOverflowTextColor(int overflowTextColor) {
        this.overflowTextColor = overflowTextColor;
        invalidate();
        return this;
    }

    public DatePickerWheelView setContentTextColor(int contentTextColor) {
        this.contentTextColor = contentTextColor;
        invalidate();
        return this;
    }

    public DatePickerWheelView setInitialDate(String initialDate) {
        this.initialDate = initialDate;
        setInitialPositions();
        invalidate();
        return this;
    }

    public DatePickerWheelView setFontTypeface(String fontTypeface) {
        yearSpinner.setFontTypeface(fontTypeface);
        monthSpinner.setFontTypeface(fontTypeface);
        daySpinner.setFontTypeface(fontTypeface);
        return this;
    }
}
