import torch.nn.functional as F
from torch import nn
from fl_core.utils.tools import calc_feat_size


class CNN_1_SSA(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_1_SSA, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)
        self.fc1 = nn.Linear(2 * n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 2000)
        self.fc2 = nn.Linear(2000, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        logits = self.fc3(rep)
        return logits, rep


class CNN_2_SSA(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_2_SSA, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, n_kernels, 5)
        self.fc1 = nn.Linear(n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 2000)
        self.fc2 = nn.Linear(2000, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        logits = self.fc3(rep)
        return logits, rep


class CNN_3_SSA(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_3_SSA, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)
        self.fc1 = nn.Linear(2 * n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 1000)
        self.fc2 = nn.Linear(1000, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        logits = self.fc3(rep)
        return logits, rep


class CNN_4_SSA(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_4_SSA, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)
        self.fc1 = nn.Linear(2 * n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 800)
        self.fc2 = nn.Linear(800, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        logits = self.fc3(rep)
        return logits, rep


class CNN_5_SSA(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_5_SSA, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)
        self.fc1 = nn.Linear(2 * n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 500)
        self.fc2 = nn.Linear(500, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        logits = self.fc3(rep)
        return logits, rep
