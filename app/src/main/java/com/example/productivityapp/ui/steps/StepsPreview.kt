package com.example.productivityapp.ui.steps

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.productivityapp.ui.theme.ProductivityAppTheme

@Preview(showBackground = true)
@Composable
fun StepsPreviewZero() {
    ProductivityAppTheme {
        StepsHeader(steps = 0, dailyGoal = 10000)
    }
}

@Preview(showBackground = true)
@Composable
fun StepsPreviewPartial() {
    ProductivityAppTheme {
        StepsProgress(steps = 4200, dailyGoal = 10000)
    }
}

@Preview(showBackground = true)
@Composable
fun StepsPreviewGoalReached() {
    ProductivityAppTheme {
        StepsProgress(steps = 12000, dailyGoal = 10000)
    }
}

