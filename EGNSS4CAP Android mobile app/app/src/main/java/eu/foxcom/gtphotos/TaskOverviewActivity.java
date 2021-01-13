package eu.foxcom.gtphotos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import eu.foxcom.gtphotos.model.LoggedUser;
import eu.foxcom.gtphotos.model.Task;
import eu.foxcom.gtphotos.model.TaskList;

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
            photoCountTextView.setText(getString(R.string.to_photoCount, task.getPhotoCount(MS.getAppDatabase())));
            TextView createdTextView = convertView.findViewById(R.id.tl_textView_createdValue);
            createdTextView.setText(task.getDateCreated() == null ? "" : task.getDateCreated());
            TextView dueDateTextView = convertView.findViewById(R.id.tl_textView_dueDateValue);
            dueDateTextView.setText(task.getTaskDueToDate() == null ? "" : task.getTaskDueToDate());
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_overview);
        initFilter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serviceController.isServiceInitialized()) {
            refreshList(false);
        }
    }

    @Override
    public void serviceInit() {
        super.serviceInit();
        refreshList(false);
    }

    @Override
    protected MainService.BROADCAST_MSG broadcastReceiver(Context context, Intent intent) {
        MainService.BROADCAST_MSG broadcastMsg = super.broadcastReceiver(context, intent);
        switch (broadcastMsg) {
            case REFRESH_TASKS:
                boolean success = intent.getBooleanExtra(MainService.BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false);
                if (success) {
                    restartActivity();
                }
                break;
        }

        return broadcastMsg;
    }

    public void refreshList(boolean filterChanged) {
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
                // způsobovalo neznámou chybu na nějakých zažízeních neznámou chybu na nějakých zařízení
                /*
                if (task.getStatus().equals(Task.STATUS.NEW.DB_VAL)) {
                    task.setStatus(Task.STATUS.OPEN.DB_VAL);
                    task.updateStatus(MS.getAppDatabase(), TaskOverviewActivity.this, new Task.UpdateTaskReceiver() {

                        @Override
                        protected void success(AppDatabase appDatabase, Task task) {
                        }

                        @Override
                        protected void success(AppDatabase appDatabase) {
                            // not used
                        }

                        @Override
                        public void failed(String error) {
                            Toast.makeText(TaskOverviewActivity.this, getString(R.string.to_failedToOpenStatus, error), Toast.LENGTH_LONG).show();
                            task.setUploadStatus(true);
                        }

                        @Override
                        public void finish(boolean success) {
                            task.saveToDB(MS.getAppDatabase());
                            goToDetail(task);
                        }
                    }, MS.getRequestor());
                } else {
                    goToDetail(task);
                }
                /**/
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
        TextView filterCountTextView = findViewById(R.id.to_textView_filterCount);
        filterCountTextView.setText(getString(R.string.to_showCount, taskListFiltered.getTasks().size(), TaskList.countOfAllTask(MS.getAppDatabase())));
    }

    private void saveTaskFilterPerzFromUI() {
        TaskList.TaskListFilter taskListFilter = new TaskList.TaskListFilter();

        // user
        taskListFilter.setUserId(LoggedUser.createFromAppDatabase(MS.getAppDatabase()).getId());

        // name
        EditText nameEditText = findViewById(R.id.to_editText_name);
        taskListFilter.setName(nameEditText.getText().toString());

        // status
        CheckBox newCheckBox = findViewById(R.id.to_checkBox_new);
        if (newCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.NEW);
        }
        CheckBox openCheckBox = findViewById(R.id.to_checkBox_open);
        if (openCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.OPEN);
        }
        CheckBox providedCheckBox = findViewById(R.id.to_checkBox_provided);
        if (providedCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.PROVIDED);
        }
        CheckBox returnedCheckBox = findViewById(R.id.to_checkBox_returned);
        if (returnedCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.RETURNED);
        }
        CheckBox acceptedCheckBox = findViewById(R.id.to_checkBox_accepted);
        if (acceptedCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.ACCEPTED);
        }
        CheckBox declinedCheckBox = findViewById(R.id.to_checkBox_declined);
        if (declinedCheckBox.isChecked()) {
            taskListFilter.getFilterStatuses().add(TaskList.TaskListFilter.STATUS.DECLINED);
        }

        // sort
        CheckBox passedAtEndCheckBox = findViewById(R.id.to_checkBox_passedAtEnd);
        taskListFilter.setSortPassedAtEnd(passedAtEndCheckBox.isChecked());
        RadioGroup sortOrderRadioGroup = findViewById(R.id.to_radioGroup_sortOrder);
        int sortOrderRadioId = sortOrderRadioGroup.getCheckedRadioButtonId();
        switch (sortOrderRadioId) {
            case R.id.to_radioButton_sortDesc:
                taskListFilter.setSortDesc(true);
                break;
            case R.id.to_radioButton_sortAsc:
                taskListFilter.setSortDesc(false);
                break;
        }
        RadioGroup sortTypeRadioGroup = findViewById(R.id.to_radioGroup_sortType);
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
        EditText nameEditText = findViewById(R.id.to_editText_name);
        nameEditText.setText(taskListFilter.getName());

        // status
        CheckBox newCheckBox = findViewById(R.id.to_checkBox_new);
        newCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.NEW));
        CheckBox openCheckBox = findViewById(R.id.to_checkBox_open);
        openCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.OPEN));
        CheckBox providedCheckBox = findViewById(R.id.to_checkBox_provided);
        providedCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.PROVIDED));
        CheckBox returnedCheckBox = findViewById(R.id.to_checkBox_returned);
        returnedCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.RETURNED));
        CheckBox acceptedCheckBox = findViewById(R.id.to_checkBox_accepted);
        acceptedCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.ACCEPTED));
        CheckBox declinedCheckBox = findViewById(R.id.to_checkBox_declined);
        declinedCheckBox.setChecked(taskListFilter.getFilterStatuses().contains(TaskList.TaskListFilter.STATUS.DECLINED));

        // sort
        CheckBox passedAtEndCheckBox = findViewById(R.id.to_checkBox_passedAtEnd);
        passedAtEndCheckBox.setChecked(taskListFilter.isSortPassedAtEnd());
        RadioGroup sortOrderRadioGroup = findViewById(R.id.to_radioGroup_sortOrder);
        int sortOrderRadioId = taskListFilter.isSortDesc() ? R.id.to_radioButton_sortDesc : R.id.to_radioButton_sortAsc;
        sortOrderRadioGroup.check(sortOrderRadioId);
        RadioGroup sortTypeRadioGroup = findViewById(R.id.to_radioGroup_sortType);
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

    public void toggleFilterShow(View view) {
        filterShown = !filterShown;
        filterShow();
    }

    private void filterShow() {
        ImageButton imageButton = findViewById(R.id.to_imageButton_showFilter);
        ConstraintLayout constraintLayout = findViewById(R.id.to_constraintLayout_filter);
        TextView filterCountTextView = findViewById(R.id.to_textView_filterCount);
        ((ViewGroup) filterCountTextView.getParent()).removeView(filterCountTextView);
        if (filterShown) {
            imageButton.setImageResource(R.drawable.icon_arrow_up);
            constraintLayout.setVisibility(View.VISIBLE);
            LinearLayout linearLayout = findViewById(R.id.to_linearLayout_innerFilterCount);
            linearLayout.addView(filterCountTextView);
        } else {
            imageButton.setImageResource(R.drawable.icon_arrow_down);
            constraintLayout.setVisibility(View.GONE);
            LinearLayout linearLayout = findViewById(R.id.to_linearLayout_headerFilterCount);
            linearLayout.addView(filterCountTextView);
        }
    }

    public void filterChanged(View view) {
        saveTaskFilterPerzFromUI();
        if (serviceController.isServiceInitialized()) {
            refreshList(true);
        }
    }



    @SuppressLint("ClickableViewAccessibility")
    private void initFilter() {
        EditText nameEditText = findViewById(R.id.to_editText_name);
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

        ConstraintLayout constraintLayout = findViewById(R.id.to_constraintLayout_filter);
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) constraintLayout.getLayoutParams();
        origHeight = layoutParams.height;
        constraintLayout.setOnTouchListener((v, event) -> {
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) constraintLayout.getLayoutParams();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                previousHeight = constraintLayout.getHeight();
                diffHeight = previousHeight - (int) event.getY();
            }
            int newHeight = (int) event.getY() + diffHeight;
            if (newHeight < previousHeight && newHeight > 0) {
                lp.height = newHeight;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (constraintLayout.getHeight() <= previousHeight / 2) {
                    toggleFilterShow(v);
                }
                lp.height = origHeight;
            }
            constraintLayout.setLayoutParams(lp);
            return true;
        });
        filterShow();
    }


}
