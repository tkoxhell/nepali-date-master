package com.puskal.merocalendar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import com.puskal.merocalendar.calendarcore.miti.Date
import com.puskal.merocalendar.calendarcore.miti.DateUtils
import com.puskal.merocalendar.databinding.LayoutCalendarWithEventBinding
import com.puskal.merocalendar.enum.CalendarType
import com.puskal.merocalendar.enum.LocalizationType
import com.puskal.merocalendar.model.DateModel
import com.puskal.merocalendar.model.EventModel
import java.text.DecimalFormat
import java.util.*

/**@author Puskal Khadka
 * 3 july, 2021
 */

class MeroCalendarView : LinearLayout {
    private var calendarType: CalendarType = CalendarType.AD
    private var language: LocalizationType = LocalizationType.ENGLISH_US
    private lateinit var binding: LayoutCalendarWithEventBinding
    private var eventList: ArrayList<EventModel> = arrayListOf()
    private var monthChangeListener: MonthChangeListener? = null
    private var dateClickListener: DateClickListener? = null
    private var weekendDay: Int = 7
    private var currentMonthDateList = arrayListOf<DateModel>()
    private var disableNextInCurrentMonth: Boolean = false
    private var disablePreviousInFirstMonth: Boolean = false

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        loadUi(context, attrs)
    }

    private fun loadUi(context: Context, attrs: AttributeSet?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_calendar_with_event, this, true)
    }

    /**
     * For showing English (Ad) or nepali (BS) calendar
     * @param CalendarType -> CalendarType.AD for english calendar, CalendarType.BS for nepal(bs) calendar
     */
    fun setCalendarType(type: CalendarType): MeroCalendarView {
        this.calendarType = type
        return this
    }

    /**
     * For setting language of calendar
     * @param LocalizationType -> LocalizationType.ENGLISH_US for english language, LocalizationType.NEPALI_NP for nepali language
     */
    fun setLanguage(lan: LocalizationType): MeroCalendarView {
        this.language = lan
        return this
    }

    /**
     * set a callback which is invoked when month is changed
     */
    fun setOnMonthChangeListener(listener: MonthChangeListener): MeroCalendarView {
        monthChangeListener = listener
        return this
    }

    /**
     * set a callback which is invoked when date of a month is clicked
     */
    fun setOnDateClickListener(listener: DateClickListener): MeroCalendarView {
        dateClickListener = listener
        return this
    }

    /**
     * set weekend day because saturday is not weekend in all country
     */
    fun setWeekendDay(day: Int): MeroCalendarView {
        weekendDay = day
        return this
    }


    private val calAdapter: EventCalendarAdapter by lazy {
        EventCalendarAdapter(dateClickListener, weekendDay)
    }

    private fun todayMonthYear(calendarInstance: Calendar): Pair<Int, Int> {
        val calendarInstance = Calendar.getInstance()
        var currentMonth = 0
        var currentYear = 0
        when (calendarType) {
            CalendarType.AD -> {
                currentMonth = calendarInstance.get(Calendar.MONTH).plus(1)
                currentYear = calendarInstance.get(Calendar.YEAR)
            }

            else -> {
                val todayNepaliDate = DateUtils.getNepaliDate(Date(calendarInstance))
                currentMonth = todayNepaliDate.month
                currentYear = todayNepaliDate.year

            }
        }
        return Pair(currentMonth, currentYear)


    }

    private fun initCalendar() {
        val calendar = Calendar.getInstance()
        val todayMonthYrs = todayMonthYear(calendar)
        var currentMonth = todayMonthYrs.first
        var currentYear = todayMonthYrs.second

        setAdapter(currentMonth, currentYear, true)

        binding.rvCalendar.apply {
            adapter = calAdapter
        }

        binding.ivArrowLeft.setOnClickListener {
            if (currentMonth == 1) {
                currentMonth = 12
                currentYear -= 1
            } else {
                currentMonth -= 1
            }
            setAdapter(currentMonth, currentYear, true)
        }

        binding.ivArrowRight.setOnClickListener {
            if (binding.ivArrowRight.isEnabled) {
                if (currentMonth == 12) {
                    currentMonth = 1
                    currentYear += 1
                } else {
                    currentMonth += 1
                }
                setAdapter(currentMonth, currentYear, true)
            }
        }
        with(binding) {
            tvToday.setOnClickListener {
                var today = todayMonthYear(calendar)
                currentMonth = today.first
                currentYear = today.second
                setAdapter(currentMonth, currentYear, true)

            }
        }
    }


    private fun setAdapter(currentMonth: Int, currentYear: Int, isMonthChange: Boolean = false) {
        val (dateList, title) = CalendarController.getDateList(
            calendarType,
            language,
            currentMonth,
            currentYear
        )

        currentMonthDateList.clear()
        currentMonthDateList.addAll(dateList)
        binding.tvDate.text = title

        val validDateList = dateList.filter { it.todayWeekDay != 0 }
        if (isMonthChange) {
            monthChangeListener?.onMonthChange(
                validDateList.first(),
                validDateList.last(),
                currentYear,
                currentMonth
            )
        }
        setEvent(eventList)  //set date in adapter + set event if available

        updateArrowsState(
            currentMonth = currentMonth,
            currentYear = currentYear
        )

    }

    /**
     * Set event to the calendar
     * @param eventList arraylist of [EventModel]
     */
    val decFormat = DecimalFormat("00")
    fun setEvent(eventList: ArrayList<EventModel>): MeroCalendarView {
        this.eventList = eventList
        if (currentMonthDateList.isNotEmpty()) {
            for (event in eventList) {
                val fromDate = event.FromDate.substringBefore("T").split("-")
                if (fromDate.size != 3) {
                    continue
                }
                val from_y = fromDate[0].toInt()
                val from_m = fromDate[1].toInt()
                val from_d = fromDate[2].toInt()

                val toDate = event.toDate.substringBefore("T").split("-")
                if (toDate.size != 3) {
                    continue
                }
                val to_y = toDate[0].toInt()
                val to_m = toDate[1].toInt()
                val to_d = toDate[2].toInt()

                val fromDateLong =
                    "$from_y${decFormat.format(from_m)}${decFormat.format(from_d)}".toLong()
                val toDateLong = "$to_y${decFormat.format(to_m)}${decFormat.format(to_d)}".toLong()

                for (dateModel in currentMonthDateList) {

                    val date = dateModel.formattedAdDate.split("-")

                    if (date.size != 3) {
                        continue
                    }
                    val date_y = date[0].toInt()
                    val date_m = date[1].toInt()
                    val date_d = date[2].toInt()

                    val currentDateLong =
                        "$date_y${decFormat.format(date_m)}${decFormat.format(date_d)}".toLong()
                    if (currentDateLong in fromDateLong..toDateLong) {
                        dateModel.hasEvent = true
                        dateModel.eventColorCode = event.colorCode
                        dateModel.isHoliday = event.isHolidayEvent
                    }
                }

            }
            calAdapter.addItem(currentMonthDateList)
        }
        return this
    }


    /**
     * Build calendar with given configuration
     */
    fun build() {
        initCalendar()
    }

    fun changeMonth(
        invokeListener: Boolean,
        month: Int,
        year: Int = todayMonthYear(Calendar.getInstance()).second
    ) {
        setAdapter(month, year, invokeListener)
    }

    fun disableNextMonthInCurrentMonth(disable: Boolean): MeroCalendarView {
        this.disableNextInCurrentMonth = disable
        return this
    }

    fun disablePreviousMonthInFirstMonth(disable: Boolean): MeroCalendarView {
        this.disablePreviousInFirstMonth = disable
        return this
    }

    private fun updateArrowsState(currentMonth: Int, currentYear: Int) {
        val today = todayMonthYear(Calendar.getInstance())

        // Disable back arrow if it's the first month of the year and the option to disable it is enabled
        if (disablePreviousInFirstMonth && currentMonth == 1 && currentYear == today.second) {
            binding.ivArrowLeft.isEnabled = false
        } else {
            binding.ivArrowLeft.isEnabled = true
        }

        // Disable forward arrow if it's the current month and year, and disableNextInCurrentMonth is true
        if (disableNextInCurrentMonth && currentMonth == today.first && currentYear == today.second) {
            binding.ivArrowRight.isEnabled = false
        } else {
            binding.ivArrowRight.isEnabled = true
        }
    }

}
