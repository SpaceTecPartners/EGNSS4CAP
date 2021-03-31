package eu.foxcom.gtphotos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import eu.foxcom.gtphotos.model.FilterTaskDialogFragment;
import eu.foxcom.gtphotos.model.LoggedUser;
import eu.foxcom.gtphotos.model.Task;
import eu.foxcom.gtphotos.model.TaskList;
import eu.foxcom.gtphotos.model.Util;
import eu.foxcom.gtphotos.model.functionInterface.Function;

public class TaskOverviewActivity extends BaseActivity {

    class TaskListAdapter extends ArrayAdapter<Task> {

        private TaskList taskList;

        public TaskListAdapter(@NonNull Context context, TaskList taskList) {
            super(context, R.layout.task_list_item, R.id.tl_textView_dummy, taskList.getTasks());
            this.taskList = taskList;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.task_list_item, parent, false);
            }

            Task task = taskList.getTasks().get(position);
            TextView nameTextView = convertView.findViewById(R.id.tl_textView_name);
            nameTextView.setText(task.getName());
            TextView statusTextView = convertView.findViewById(R.id.tl_textView_status);
            Task.STATUS status = Task.STATUS.createFromDBVal(task.getStatus());
            statusTextView.setText(getString(status.NAME_ID));
            ImageView statusImageView = convertView.findViewById(R.id.tl_imageView_status);
            statusImageView.setVisibility(View.VISIBLE);
            if (status.equals(Task.STATUS.DATA_CHECKED)) {
                if (task.getFlagValid() && !task.getFlagInvalid()) {
                    statusImageView.setImageResource(R.drawable.point_green);
                } else if (!task.getFlagValid() && task.getFlagInvalid()) {
                    statusImageView.setImageResource(R.drawable.point_red);
                } else {
                    statusImageView.setVisibility(View.GONE);
                    statusTextView.setText(getString(R.string.stat_dataCheckedError));
                }
            } else {
                statusImageView.setImageResource(status.POINT_ID);
            }
            TextView photoCountTextView = convertView.findViewById(R.id.tl_textView_photoCount);
            int count = task.getPhotoCount(MS.getAppDatabase());
            if (count == 1) {
                photoCountTextView.setText(getString(R.string.to_photoCount_1, count));
            } else if (count >= 2 && count <= 4) {
                photoCountTextView.setText(getString(R.string.to_photoCount_2, count));
            } else {
                photoCountTextView.setText(getString(R.string.to_photoCount_3, count));
            }
            TextView createdTextView = convertView.findViewById(R.id.tl_textView_createdValue);
            DateTimeFormatter dateFormatter = Util.createPrettyDateFormat();
            DateTimeFormatter timeFormatter = Util.createPrettyTimeFormat();
            Function<DateTime, String> prettyDateTime = dateTime -> {
                if (dateTime == null) {
                    return "";
                }
                return dateTime.toString(dateFormatter) + " " + dateTime.toString(timeFormatter);
            };
            createdTextView.setText(prettyDateTime.apply(task.getDateCreatedDateTime()));
            TextView dueDateTextView = convertView.findViewById(R.id.tl_textView_dueDateValue);
            dueDateTextView.setText(prettyDateTime.apply(task.getTaskDueToDateDateTime()));
            if (task.getTaskDueToDateDatetime() != null && task.getTaskDueToDateDatetime().isBeforeNow()) {
                dueDateTextView.setTextColor(Color.RED);
            } else {
                dueDateTextView.setTextColor(Color.rgb(0, 150, 0));
            }
            return super.getView(position, convertView, parent);
        }
    }

    boolean filterShown = false;
    int origHeight;
    int previousHeight;
    int diffHeight;
    TaskList.TaskListFilter taskListFilter;
    boolean isFilterSettingRun = false;
    private Integer firstVisibleItem;

    private FilterTaskDialogFragment filterDialog;
    private View filterView;
    private boolean isFilterViewInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_overview);
        setToolbar(R.id.toolbar);

        filterDialog = new FilterTaskDialogFragment(this);
        filterView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_task_overview, null, false);
        filterDialog.show(getSupportFragmentManager(), FilterTaskDialogFragment.TAG);
        filterDialog.dismiss();
        EditText filterEditText = findViewById(R.id.to_editText_filter);
        filterEditText.setOnClickListener(v -> filterDialog.show(getSupportFragmentManager(), FilterTaskDialogFragment.TAG));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList(false);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();
        refreshList(false);
    }

    @Override
    protected MainService.BROADCAST_MSG broadcastImplicitReceiver(Context context, Intent intent) {
        MainService.BROADCAST_MSG broadcastMsg = super.broadcastImplicitReceiver(context, intent);
        switch (broadcastMsg) {
            case REFRESH_TASKS_FINISHED:
                boolean success = intent.getBooleanExtra(MainService.BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false);
                if (success) {
                    restartActivity();
                }
                break;
        }

        return broadcastMsg;
    }

    public void refreshList(boolean filterChanged) {
        if (!serviceController.isServiceBound() || !isFilterViewInit) {
            return;
        }

        ListView listView = findViewById(R.id.to_listView);
        if (taskListFilter == null) {
            loadTaskFilterPerzToUI();
        }
        if (taskListFilter == null) {
            saveTaskFilterPerzFromUI();
        }
        final TaskList taskListFiltered = TaskList.createFromAppDatabaseFilter(MS.getAppDatabase(), taskListFilter);
        TaskListAdapter taskListAdapter = new TaskListAdapter(this, taskListFiltered);
        listView.setAdapter(taskListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Task task = taskListFiltered.getTasks().get(position);
                if (task.getStatus().equals(Task.STATUS.NEW.DB_VAL)) {
                    task.setStatus(Task.STATUS.OPEN.DB_VAL);
                    task.setUploadStatus(true);
                    task.saveToDB(MS.getAppDatabase());
                }
                goToDetail(task);
            }
        });
        if (filterChanged) {
            firstVisibleItem = null;
        } else {
            if (firstVisibleItem != null) {
                listView.setSelection(firstVisibleItem);
            }
        }
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                TaskOverviewActivity.this.firstVisibleItem = firstVisibleItem;
            }
        });
        int shownCount = taskListFiltered.getTasks().size();
        int totalCount = TaskList.countOfAllTask(MS.getAppDatabase());
        TextView filterCountTextView = filterView.findViewById(R.id.to_textView_filterCount);
        filterCountTextView.setText(getString(R.string.to_showCount, shownCount, totalCount));
        EditText editTextFilter = findViewById(R.id.to_editText_filter);
        editTextFilter.setHint(getString(R.string.to_filter) + " (" + getString(R.string.to_showCount, shownCount, totalCount) + ")");
    }

    private void saveTaskFilterPerzFromUI() {
        TaskList.TaskListFilter taskListFilter = new TaskList.TaskListFilter();

        // user
        taskListFilter.setUserId(LoggedUser.createFromAppDatabase(MS.getAppDatabase()).getId());

        // name
        EditText nameEditText = filterView.findViewById(R.id.to_editText_name);
        taskListFilter.setName(nameEditText.getText().toString());

        // status
        CheckBox newCheckBox = filterView.findViewById(R.id.to_checkBox_new);
        if (newCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.NEW);
        }
        CheckBox openCheckBox = filterView.findViewById(R.id.to_checkBox_open);
        if (openCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.OPEN);
        }
        CheckBox providedCheckBox = filterView.findViewById(R.id.to_checkBox_provided);
        if (providedCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.PROVIDED);
        }
        CheckBox returnedCheckBox = filterView.findViewById(R.id.to_checkBox_returned);
        if (returnedCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.RETURNED);
        }
        CheckBox acceptedCheckBox = filterView.findViewById(R.id.to_checkBox_accepted);
        if (acceptedCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.ACCEPTED);
        }
        CheckBox declinedCheckBox = filterView.findViewById(R.id.to_checkBox_declined);
        if (declinedCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.DECLINED);
        }

        // sort
        CheckBox passedAtEndCheckBox = filterView.findViewById(R.id.to_checkBox_passedAtEnd);
        taskListFilter.setSortPassedAtEnd(passedAtEndCheckBox.isChecked());
        RadioGroup sortOrderRadioGroup = filterView.findViewById(R.id.to_radioGroup_sortOrder);
        int sortOrderRadioId = sortOrderRadioGroup.getCheckedRadioButtonId();
        switch (sortOrderRadioId) {
            case R.id.to_radioButton_sortDesc:
                taskListFilter.setSortDesc(true);
                break;
            case R.id.to_radioButton_sortAsc:
                taskListFilter.setSortDesc(false);
                break;
        }
        RadioGroup sortTypeRadioGroup = filterView.findViewById(R.id.to_radioGroup_sortType);
        int sortTypeId = sortTypeRadioGroup.getCheckedRadioButtonId();
        switch (sortTypeId) {
            case R.id.to_radioButton_sortByStatus:
                taskListFilter.setSort(TaskList.TaskListFilter.SORT.STATUS);
                break;
            case R.id.to_radioButton_sortByDueDate:
                taskListFilter.setSort(TaskList.TaskListFilter.SORT.DUE_DATE);
                break;
            case R.id.to_radioButton_sortByName:
                taskListFilter.setSort(TaskList.TaskListFilter.SORT.NAME);
                break;
        }

        this.taskListFilter = taskListFilter;
        this.taskListFilter.saveToPersistData(this);
    }

    private void loadTaskFilterPerzToUI() {
        TaskList.TaskListFilter taskListFilter = TaskList.TaskListFilter.createFromPerzData(this, LoggedUser.createFromAppDatabase(MS.getAppDatabase()).getId());
        if (taskListFilter == null) {
            this.taskListFilter = null;
            return;
        }

        isFilterSettingRun = true;

        // name
        EditText nameEditText = filterView.findViewById(R.id.to_editText_name);
        nameEditText.setText(taskListFilter.getName());

        // status
        CheckBox newCheckBox = filterView.findViewById(R.id.to_checkBox_new);
        newCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.NEW));
        CheckBox openCheckBox = filterView.findViewById(R.id.to_checkBox_open);
        openCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.OPEN));
        CheckBox providedCheckBox = filterView.findViewById(R.id.to_checkBox_provided);
        providedCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.PROVIDED));
        CheckBox returnedCheckBox = filterView.findViewById(R.id.to_checkBox_returned);
        returnedCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.RETURNED));
        CheckBox acceptedCheckBox = filterView.findViewById(R.id.to_checkBox_accepted);
        acceptedCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.ACCEPTED));
        CheckBox declinedCheckBox = filterView.findViewById(R.id.to_checkBox_declined);
        declinedCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.DECLINED));

        // sort
        CheckBox passedAtEndCheckBox = filterView.findViewById(R.id.to_checkBox_passedAtEnd);
        passedAtEndCheckBox.setChecked(taskListFilter.isSortPassedAtEnd());
        RadioGroup sortOrderRadioGroup = filterView.findViewById(R.id.to_radioGroup_sortOrder);
        int sortOrderRadioId = taskListFilter.isSortDesc() ? R.id.to_radioButton_sortDesc : R.id.to_radioButton_sortAsc;
        sortOrderRadioGroup.check(sortOrderRadioId);
        RadioGroup sortTypeRadioGroup = filterView.findViewById(R.id.to_radioGroup_sortType);
        TaskList.TaskListFilter.SORT sort = taskListFilter.getSort();
        switch (sort) {
            case STATUS:
                sortTypeRadioGroup.check(R.id.to_radioButton_sortByStatus);
                break;
            case DUE_DATE:
                sortTypeRadioGroup.check(R.id.to_radioButton_sortByDueDate);
                break;
            case NAME:
                sortTypeRadioGroup.check(R.id.to_radioButton_sortByName);
                break;
        }

        this.taskListFilter = taskListFilter;

        isFilterSettingRun = false;
    }

    private void goToDetail(Task task) {
        String taskId = task.getId();
        Intent intent = new Intent(TaskOverviewActivity.this, TaskFulfillActivity.class);
        intent.setAction(TaskFulfillActivity.INTENT_ACTION_START);
        intent.putExtra(TaskFulfillActivity.INTENT_ACTION_START_TASK_ID, taskId);
        startActivity(intent);
    }

    public void filterChanged(View view) {
        saveTaskFilterPerzFromUI();
        refreshList(true);
    }


    @SuppressLint("ClickableViewAccessibility")
    public void initFilter() {
        EditText nameEditText = filterView.findViewById(R.id.to_editText_name);
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)     {
                if (isFilterSettingRun) {
                    return;
                }
                filterChanged(getWindow().getDecorView().getRootView());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        isFilterViewInit = true;
        refreshList(false);
    }

    // region get, set

    public View getFilterView() {
        return filterView;
    }

    // endregion


}


/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */